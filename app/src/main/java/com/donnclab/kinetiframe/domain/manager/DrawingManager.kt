package com.donnclab.kinetiframe.domain.manager

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import com.donnclab.kinetiframe.engine.cache.FrameCacheManager
import com.donnclab.kinetiframe.engine.model.DrawCommand
import com.donnclab.kinetiframe.engine.model.DrawingTool
import com.donnclab.kinetiframe.engine.model.TouchPoint
import com.donnclab.kinetiframe.engine.math.StrokeGeometry
import com.donnclab.kinetiframe.engine.drawing.BrushRenderers
import com.donnclab.kinetiframe.engine.drawing.BrushSnapshot
import com.donnclab.kinetiframe.engine.drawing.BlurTool
import com.donnclab.kinetiframe.engine.drawing.SmudgeTool
import com.donnclab.kinetiframe.engine.drawing.ManipulationStrokeRenderer
import com.donnclab.kinetiframe.engine.ruler.RulerStrokeConstraintSession
import com.donnclab.kinetiframe.ui.components.canvas.toConstraintSpec
import com.donnclab.kinetiframe.ui.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.*

/**
 * Manager for handling drawing logic, including stroke baking, undo/redo, and ruler application.
 */
class DrawingManager(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val cacheManager: FrameCacheManager,
    private val bakeMutex: Mutex,
    private val frameOperationMutex: Mutex,
    private val getState: () -> TimelineState,
    private val updateState: ((TimelineState) -> TimelineState) -> Unit,
    private val getBaseBitmap: () -> Bitmap?,
    private val setBaseBitmap: (Bitmap?) -> Unit,
    private val activeLayerId: () -> String,
    private val composeFrame: suspend (frameIndex: Int, layerId: String, layerBitmap: Bitmap?) -> Bitmap?,
    private val history: MutableList<HistoryItem>,
    private val redoHistory: MutableList<HistoryItem>,
    private val acquireBitmap: (Int, Int) -> Bitmap,
    private val releaseBitmap: (Bitmap?) -> Unit,
    /** Fired after a layer's frame bitmap settles (stroke bake, undo/redo, ...). Used to drive
     *  "tap a bone, then sketch" auto-binding -- see [com.donnclab.kinetiframe.domain.manager.RiggingManager.autoBindLayerContent]. */
    private val onFrameBaked: (layerId: String, bitmap: Bitmap) -> Unit = { _, _ -> },
    private val pendingOperations: PendingOperations = PendingOperations()
) {
    private val MAX_HISTORY = 20
    private val selectionHistoryBudget = minOf(24L * 1024 * 1024, Runtime.getRuntime().maxMemory() / 12)
    private val blurTool = BlurTool()
    private val smudgeTool = SmudgeTool()
    // Raster commands are meaningful only for the layer and frame they were created on. Sharing
    // this session across auto-selected body-part layers rebuilt torso/leg commands into the next
    // body part, then made every bound texture animate at once.
    private var sessionLayerId: String? = null
    private var sessionFrameIndex: Int? = null

    private fun retireStrokeOverlay(seed: Long) {
        updateState { state -> state.copy(bakedStrokeSeeds =
            com.donnclab.kinetiframe.engine.model.StrokeBakeReceipts.acknowledge(state.bakedStrokeSeeds, seed)) }
    }

    fun bakeStroke(points: List<TouchPoint>, brushSnapshot: BrushSnapshot? = null, targetLayerId: String? = null) {
        if (points.isEmpty()) return
        // Capture identity before suspension: queue delays must never redirect a finished mark.
        val submittedState = getState()
        val submittedConfig = submittedState.tool.activeConfig
        val submission = com.donnclab.kinetiframe.engine.model.StrokeSubmission.capture(
            DrawCommand(points, submittedState.tool.activeTool, submittedConfig.color.toArgb(),
                submittedConfig.size, submittedConfig.opacity, submittedConfig.strength,
                brushSnapshot ?: BrushSnapshot(id = submittedConfig.brushId, seed = points.first().timestamp)),
            submittedState.animation.currentFrame, targetLayerId ?: activeLayerId()
        )
        val submittedPoints = submission.command.points
        val submittedLayerId = submission.layerId
        val submittedFrameIndex = submission.frameIndex
        val submittedLayer = submittedState.currentFrameLayers().firstOrNull { it.id == submittedLayerId }
        if (submittedLayer == null || submittedLayer.isLocked) {
            retireStrokeOverlay(submission.command.brush.seed)
            return
        }
        pendingOperations.launch(scope, dispatcher) {
            frameOperationMutex.withLock {
                val layerId = submittedLayerId
                val latest = getState()
                val targetLayers = latest.layers.frameLayers[submittedFrameIndex] ?: latest.layers.layers
                if (targetLayers.none { it.id == layerId }) {
                    retireStrokeOverlay(submission.command.brush.seed)
                    return@withLock
                }
                // A lock toggled after submission does not retroactively discard an accepted mark.
                if (submittedFrameIndex !in 0 until latest.animation.totalFrames) {
                    retireStrokeOverlay(submission.command.brush.seed)
                    return@withLock
                }
                prepareLayerSession(layerId, submittedFrameIndex)
                val drawingState = submittedState
                val config = drawingState.tool.activeConfig
                if (drawingState.tool.activeTool == DrawingTool.ERASER && config.eraserType == EraserType.STROKE) {
                    val eraserPoints = applyRuler(submittedPoints, drawingState.canvas.rulerGuide)
                    val eraserSize = config.size
                    
                    var changed = false
                    val iterator = history.iterator()
                    while (iterator.hasNext()) {
                        val item = iterator.next()
                        val strokes = when (item) {
                            is HistoryItem.Stroke -> listOf(item.command)
                            is HistoryItem.StrokeBatch -> item.commands
                            else -> emptyList()
                        }
                        if (strokes.any { stroke -> stroke.tool != DrawingTool.ERASER && intersects(stroke, eraserPoints, eraserSize) }) {
                                iterator.remove()
                                changed = true
                        }
                    }
                    
                    if (changed) {
                        rebuildCurrentBitmap(layerId, submittedFrameIndex, submission.command.brush.seed)
                    } else {
                        retireStrokeOverlay(submission.command.brush.seed)
                    }
                    restoreVisibleSession(layerId, submittedFrameIndex)
                    return@withLock
                }

                // Brush input is already constrained before it reaches the live renderer, so it
                // must not be projected again at bake time; that would distort rotated ellipses
                // and boxes.
                // Mirror uses the same primary stream and commits its reflection separately.
                val commandPoints = if (
                    drawingState.tool.activeTool == DrawingTool.BRUSH
                ) {
                    submittedPoints
                } else {
                    applyRuler(submittedPoints, drawingState.canvas.rulerGuide)
                }
                val command = submission.command.copy(
                    points = commandPoints,
                    cachedPath = null
                )

                val guide = drawingState.canvas.rulerGuide
                if (drawingState.tool.activeTool == DrawingTool.BRUSH && guide?.mode == RulerMode.MIRROR) {
                    val reflection = RulerStrokeConstraintSession(guide.toConstraintSpec())
                    history.add(HistoryItem.StrokeBatch(listOf(command, command.copy(points = submittedPoints.map(reflection::reflect)))))
                } else {
                    history.add(HistoryItem.Stroke(command))
                }
                redoHistory.clear()

                trimHistory()

                rebuildCurrentBitmap(layerId, submittedFrameIndex, submission.command.brush.seed)
                // A late bake must not leave Undo operating on an offscreen frame's session.
                restoreVisibleSession(layerId, submittedFrameIndex)
            }
        }
    }

    /** Caller owns frameOperationMutex; cap selection bitmaps as well as brush gestures. */
    suspend fun trimHistory() {
        fun retainedPixelBytes() = history.sumOf { item ->
            val bitmap = when (item) {
                is HistoryItem.SelectionEdit -> item.bitmap
                is HistoryItem.RasterSelection -> item.stamp?.bitmap
                else -> null
            }
            bitmap?.takeUnless(Bitmap::isRecycled)?.allocationByteCount?.toLong() ?: 0L
        }
        while (history.size > MAX_HISTORY || (history.size > 1 && retainedPixelBytes() > selectionHistoryBudget)) {
            when (val oldest = history.removeAt(0)) {
                is HistoryItem.Stroke -> bakeToBase(oldest.command)
                is HistoryItem.StrokeBatch -> oldest.commands.forEach { bakeToBase(it) }
                is HistoryItem.SelectionEdit -> applySelectionToBase(oldest)
                is HistoryItem.RasterSelection -> applyRasterSelectionToBase(oldest)
                is HistoryItem.ClearArea -> applyRasterSelectionToBase(HistoryItem.RasterSelection(oldest, null))
                is HistoryItem.ObjectEdit -> Unit
            }
        }
    }

    private suspend fun restoreVisibleSession(layerId: String, frameIndex: Int) {
        val visible = getState()
        val visibleLayerId = activeLayerId()
        if (visible.animation.currentFrame != frameIndex || visibleLayerId != layerId) {
            prepareLayerSession(visibleLayerId, visible.animation.currentFrame)
        }
    }

    /** Called by the normal frame loader after it has prepared [baseBitmap] itself. */
    fun markLayerSession(layerId: String, frameIndex: Int) {
        sessionLayerId = layerId
        sessionFrameIndex = frameIndex
    }

    /**
     * Opens an isolated editable bitmap for one layer. The actor canvas may display a composite
     * drawing guide, but only this layer's pixels are eligible for its bone texture.
     */
    private suspend fun prepareLayerSession(layerId: String, frameIndex: Int) {
        if (sessionLayerId == layerId && sessionFrameIndex == frameIndex) return
        val layerBitmap = cacheManager.getFrame(layerId, frameIndex)
        setBaseBitmap(layerBitmap?.copy(Bitmap.Config.ARGB_8888, true))
        history.clear()
        redoHistory.clear()
        sessionLayerId = layerId
        sessionFrameIndex = frameIndex
    }

    /** Commits all strokes generated by one construction gesture as one undoable history item. */
    fun bakeConstructionShape(strokes: List<List<TouchPoint>>) {
        if (strokes.all { it.isEmpty() }) return
        val state = getState()
        val layerId = activeLayerId()
        val frameIndex = state.animation.currentFrame
        val layer = state.currentFrameLayers().firstOrNull { it.id == layerId } ?: return
        if (layer.isLocked) return
        val config = state.tool.activeConfig
        val commands = strokes.filter { it.isNotEmpty() }.map { points ->
            com.donnclab.kinetiframe.engine.model.StrokeSubmission.capture(
                DrawCommand(points, DrawingTool.BRUSH, config.color.toArgb(), config.size, config.opacity,
                    config.strength, BrushSnapshot(id = config.brushId, seed = points.first().timestamp)),
                frameIndex, layerId
            ).command
        }
        pendingOperations.launch(scope, dispatcher) {
            frameOperationMutex.withLock {
                val latest = getState()
                if (frameIndex !in 0 until latest.animation.totalFrames) return@withLock
                val targetLayers = latest.layers.frameLayers[frameIndex] ?: latest.layers.layers
                if (targetLayers.none { it.id == layerId }) return@withLock
                prepareLayerSession(layerId, frameIndex)
                history.add(HistoryItem.StrokeBatch(commands))
                redoHistory.clear()
                rebuildCurrentBitmap(layerId, frameIndex)
                restoreVisibleSession(layerId, frameIndex)
            }
        }
    }

    private fun applyRuler(points: List<TouchPoint>, guide: RulerGuide?): List<TouchPoint> {
        if (guide == null || guide.mode == RulerMode.NONE || points.isEmpty()) return points
        return when (guide.mode) {
            RulerMode.LINE -> points.map { point ->
                val a = guide.start
                val b = guide.end
                val dx = b.x - a.x
                val dy = b.y - a.y
                val t = (((point.x - a.x) * dx + (point.y - a.y) * dy) / (dx * dx + dy * dy).coerceAtLeast(1f))
                point.copy(x = a.x + dx * t, y = a.y + dy * t)
            }
            RulerMode.MIRROR -> points + points.asReversed().map { point ->
                point.copy(x = guide.center.x - (point.x - guide.center.x))
            }
            RulerMode.CIRCLE -> points.map { point ->
                val angle = atan2(point.y - guide.center.y, point.x - guide.center.x)
                point.copy(
                    x = guide.center.x + cos(angle) * guide.radius.x,
                    y = guide.center.y + sin(angle) * guide.radius.y
                )
            }
            RulerMode.BOX -> points.map { point ->
                val left = guide.center.x - guide.radius.x
                val right = guide.center.x + guide.radius.x
                val top = guide.center.y - guide.radius.y
                val bottom = guide.center.y + guide.radius.y
                val distances = floatArrayOf(abs(point.x - left), abs(point.x - right), abs(point.y - top), abs(point.y - bottom))
                when (distances.indices.minBy { distances[it] }) {
                    0 -> point.copy(x = left)
                    1 -> point.copy(x = right)
                    2 -> point.copy(y = top)
                    else -> point.copy(y = bottom)
                }
            }
            RulerMode.NONE -> points
        }
    }

    private suspend fun bakeToBase(command: DrawCommand) = withContext(dispatcher) {
        bakeMutex.withLock {
            val stage = getState().project.stage
            val width = stage.width
            val height = stage.height
            val bitmap = getBaseBitmap() ?: acquireBitmap(width, height).also { setBaseBitmap(it) }
            
            val canvas = Canvas(bitmap)
            drawPressureStroke(canvas, command, bitmap)
        }
    }

    private suspend fun applySelectionToBase(edit: HistoryItem.SelectionEdit) = withContext(dispatcher) {
        bakeMutex.withLock {
            val stage = getState().project.stage
            val width = stage.width
            val height = stage.height
            val bitmap = getBaseBitmap() ?: acquireBitmap(width, height).also { setBaseBitmap(it) }
            val canvas = Canvas(bitmap)
            
            if (edit.type == SelectionEditType.COMMIT || edit.type == SelectionEditType.PASTE) {
                edit.bitmap?.let { proxy ->
                    canvas.save()
                    canvas.concat(edit.matrix.toAndroidMatrix())
                    canvas.drawBitmap(proxy, edit.originalBounds.left, edit.originalBounds.top, Paint(Paint.ANTI_ALIAS_FLAG))
                    canvas.restore()
                }
            }
        }
    }

    private fun Matrix.toAndroidMatrix() = android.graphics.Matrix().also { android ->
        val v = this.values
        android.setValues(floatArrayOf(v[0], v[4], v[12], v[1], v[5], v[13], v[3], v[7], v[15]))
    }

    private fun drawRasterSelection(canvas: Canvas, edit: HistoryItem.RasterSelection) {
        edit.clear?.let { clear ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR) }
            clear.path?.let { canvas.drawPath(it.asAndroidPath(), paint) }
                ?: clear.rect?.let { canvas.drawRect(it.left, it.top, it.right, it.bottom, paint) }
        }
        edit.stamp?.let { stamp -> stamp.bitmap?.let { proxy ->
            canvas.save()
            canvas.concat(stamp.matrix.toAndroidMatrix())
            canvas.drawBitmap(proxy, stamp.originalBounds.left, stamp.originalBounds.top, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
        } }
    }

    private suspend fun applyRasterSelectionToBase(edit: HistoryItem.RasterSelection) = withContext(dispatcher) {
        bakeMutex.withLock {
            val stage = getState().project.stage
            val bitmap = getBaseBitmap() ?: acquireBitmap(stage.width, stage.height).also { setBaseBitmap(it) }
            drawRasterSelection(Canvas(bitmap), edit)
        }
    }

    suspend fun rebuildCurrentBitmap(
        targetLayerId: String = sessionLayerId ?: activeLayerId(),
        targetFrameIndex: Int = sessionFrameIndex ?: getState().animation.currentFrame,
        completedStrokeSeed: Long? = null
    ) = withContext(dispatcher) {
        bakeMutex.withLock {
            val stage = getState().project.stage
            val width = stage.width
            val height = stage.height
            val currentState = getState()
            
            val resultBitmap = acquireBitmap(width, height)
            val base = getBaseBitmap()
            if (base != null) {
                val canvas = Canvas(resultBitmap)
                canvas.drawBitmap(base, 0f, 0f, null)
            }
            
            val canvas = Canvas(resultBitmap)
            for (item in history) {
                when (item) {
                    is HistoryItem.Stroke -> drawPressureStroke(canvas, item.command, resultBitmap)
                    is HistoryItem.StrokeBatch -> item.commands.forEach { drawPressureStroke(canvas, it, resultBitmap) }
                    is HistoryItem.SelectionEdit -> {
                        if (item.type == SelectionEditType.COMMIT || item.type == SelectionEditType.PASTE) {
                            item.bitmap?.let { proxy ->
                                canvas.save()
                                canvas.concat(item.matrix.toAndroidMatrix())
                                canvas.drawBitmap(proxy, item.originalBounds.left, item.originalBounds.top, Paint(Paint.ANTI_ALIAS_FLAG))
                                canvas.restore()
                            }
                        }
                    }
                    is HistoryItem.ClearArea -> {
                        // For raster undo, if we are rebuilding, we clear the area.
                        // However, if we are UNDOING a clear, we would draw the backup.
                        // Here we are just rebuilding the current state.
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
                            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR) 
                        }
                        if (item.path != null) {
                            canvas.drawPath(item.path.asAndroidPath(), paint)
                        } else if (item.rect != null) {
                            canvas.drawRect(item.rect.left, item.rect.top, item.rect.right, item.rect.bottom, paint)
                        }
                    }
                    is HistoryItem.ObjectEdit -> Unit // Already applied to durable scene metadata.
                    is HistoryItem.RasterSelection -> drawRasterSelection(canvas, item)
                }
            }
            
            cacheManager.putFrame(targetLayerId, targetFrameIndex, resultBitmap)
            val compositeBitmap = composeFrame(targetFrameIndex, targetLayerId, resultBitmap)
            withContext(Dispatchers.Main) {
                onFrameBaked(targetLayerId, resultBitmap)
                updateState { state ->
                    val targetLayers = state.currentFrameLayers()
                    val exposures = cacheManager.exposures()
                    val cel = exposures[targetLayerId]?.get(targetFrameIndex)
                    val affectedFrames = if (cel == null) setOf(targetFrameIndex) else
                        exposures[targetLayerId].orEmpty().filterValues { it == cel }.keys
                    val updatedLayers = targetLayers.map { layer ->
                        if (layer.id == targetLayerId) {
                            layer.copy(populatedFrames = layer.populatedFrames + affectedFrames)
                        } else layer
                    }
                    state.copy(
                        currentBitmap = if (state.animation.currentFrame == targetFrameIndex) compositeBitmap else state.currentBitmap,
                        bakedStrokeSeeds = com.donnclab.kinetiframe.engine.model.StrokeBakeReceipts.acknowledge(
                            state.bakedStrokeSeeds, completedStrokeSeed),
                        layers = state.layers.copy(
                            layers = updatedLayers,
                            frameLayers = state.layers.frameLayers.mapValues { updatedLayers } + (targetFrameIndex to updatedLayers),
                            linkedCels = exposures
                        )
                    )
                }
            }
        }
    }

    fun undo() {
        if (history.isEmpty()) return
        
        pendingOperations.launch(scope, dispatcher) {
            frameOperationMutex.withLock {
                if (history.isNotEmpty()) {
                    val last = history.removeAt(history.size - 1)
                    redoHistory.add(last)
                    if (last is HistoryItem.ObjectEdit) applyObjectEdit(last, undo = true)
                    rebuildCurrentBitmap()
                }
            }
        }
    }

    fun redo() {
        if (redoHistory.isEmpty()) return
        
        pendingOperations.launch(scope, dispatcher) {
            frameOperationMutex.withLock {
                if (redoHistory.isNotEmpty()) {
                    val last = redoHistory.removeAt(redoHistory.size - 1)
                    history.add(last)
                    if (last is HistoryItem.ObjectEdit) applyObjectEdit(last, undo = false)
                    rebuildCurrentBitmap()
                }
            }
        }
    }

    private fun applyObjectEdit(edit: HistoryItem.ObjectEdit, undo: Boolean) {
        val actorKeys = edit.beforeActors.keys + edit.afterActors.keys
        val nodeKeys = edit.beforeNodes.keys + edit.afterNodes.keys
        val videoKeys = edit.beforeVideos.keys + edit.afterVideos.keys
        val audioKeys = edit.beforeAudio.keys + edit.afterAudio.keys
        updateState { state -> state.copy(
            project = state.project.copy(actorPlacements = (state.project.actorPlacements - actorKeys) + (if (undo) edit.beforeActors else edit.afterActors),
                videoClips = (state.project.videoClips - videoKeys) + (if (undo) edit.beforeVideos else edit.afterVideos),
                audioClips = (state.project.audioClips - audioKeys) + (if (undo) edit.beforeAudio else edit.afterAudio)),
            scene = state.scene.copy(nodes = (state.scene.nodes - nodeKeys) + (if (undo) edit.beforeNodes else edit.afterNodes))
        ) }
    }

    fun clearAllHandwriting() {
        pendingOperations.launch(scope, dispatcher) {
            frameOperationMutex.withLock {
                history.clear()
                redoHistory.clear()
                getBaseBitmap()?.eraseColor(android.graphics.Color.TRANSPARENT)
                rebuildCurrentBitmap()
            }
        }
    }

    private fun intersects(stroke: DrawCommand, eraserPoints: List<TouchPoint>, eraserSize: Float): Boolean {
        val strokePoints = stroke.points
        if (strokePoints.isEmpty()) return false
        
        val threshold = (stroke.width + eraserSize) / 2f
        val thresholdSq = threshold * threshold
        
        for (eP in eraserPoints) {
            for (i in 0 until strokePoints.size - 1) {
                val p1 = strokePoints[i]
                val p2 = strokePoints[i + 1]
                if (distToSegmentSquared(eP.x, eP.y, p1.x, p1.y, p2.x, p2.y) < thresholdSq) {
                    return true
                }
            }
        }
        return false
    }

    private fun distToSegmentSquared(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        if (dx == 0f && dy == 0f) return (px - x1) * (px - x1) + (py - y1) * (py - y1)
        val t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)
        return if (t < 0) {
            (px - x1) * (px - x1) + (py - y1) * (py - y1)
        } else if (t > 1) {
            (px - x2) * (px - x2) + (py - y2) * (py - y2)
        } else {
            val projX = x1 + t * dx
            val projY = y1 + t * dy
            (px - projX) * (px - projX) + (py - projY) * (py - projY)
        }
    }

    private fun drawPressureStroke(canvas: Canvas, command: DrawCommand, bitmap: Bitmap) {
        val points = command.points
        if (points.isEmpty()) return

        if (command.tool == DrawingTool.BLUR || command.tool == DrawingTool.SMUDGE) {
            ManipulationStrokeRenderer.render(
                canvas = canvas,
                bitmap = bitmap,
                points = points,
                tool = command.tool,
                baseWidth = command.width,
                strength = command.strength,
                blurTool = blurTool,
                smudgeTool = smudgeTool
            )
            return
        }

        if (command.tool == DrawingTool.COLOR_FILL) {
            val point = points.first()
            floodFill(bitmap, point.x.toInt(), point.y.toInt(), command.color, command.width)
            return
        }

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            if (command.tool == DrawingTool.ERASER) {
                xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                color = android.graphics.Color.TRANSPARENT
                alpha = 0
            } else {
                color = command.color
                alpha = (command.alpha * 255).toInt().coerceIn(0, 255)
                xfermode = null
            }
        }

        BrushRenderers.draw(canvas, points, command.width, paint, command.brush)
    }

    suspend fun generateFillPreview(offset: androidx.compose.ui.geometry.Offset): Bitmap? = withContext(dispatcher) {
        val currentState = getState()
        val stage = getState().project.stage
        val width = stage.width
        val height = stage.height
        
        // Use a clean bitmap for the preview
        val previewBitmap = acquireBitmap(width, height)
        previewBitmap.eraseColor(android.graphics.Color.TRANSPARENT)
        
        // Fill boundaries come from the active layer only: its base bitmap plus its stroke history.
        val referenceBitmap = acquireBitmap(width, height)
        referenceBitmap.eraseColor(android.graphics.Color.TRANSPARENT)
        val refCanvas = Canvas(referenceBitmap)
        getBaseBitmap()?.let { refCanvas.drawBitmap(it, 0f, 0f, null) }
        for (item in history) {
            when (item) {
                is HistoryItem.Stroke -> drawPressureStroke(refCanvas, item.command, referenceBitmap)
                is HistoryItem.StrokeBatch -> item.commands.forEach { drawPressureStroke(refCanvas, it, referenceBitmap) }
                else -> Unit
            }
        }

        val config = currentState.tool.activeConfig
        floodFill(previewBitmap, offset.x.toInt(), offset.y.toInt(), config.color.toArgb(), config.size, referenceBitmap)
        
        releaseBitmap(referenceBitmap)
        previewBitmap
    }

    private fun floodFill(bitmap: Bitmap, x: Int, y: Int, targetColor: Int, tolerance: Float, reference: Bitmap? = null) {
        val source = reference ?: bitmap
        if (x !in 0 until source.width || y !in 0 until source.height) return
        
        val startColor = source.getPixel(x, y)
        if (startColor == targetColor && reference == null) return
        
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val resultPixels = if (reference != null) IntArray(width * height) else pixels
        
        val queue = java.util.ArrayDeque<Int>()
        queue.add(y * width + x)
        
        val toleranceSq = (tolerance * 4.41f).let { it * it }
        val visited = java.util.BitSet(width * height)
        visited.set(y * width + x)
        
        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            
            if (isWithinTolerance(pixels[index], startColor, toleranceSq)) {
                resultPixels[index] = targetColor
                
                val currX = index % width
                val currY = index / width
                
                if (currX > 0 && !visited.get(index - 1)) { queue.add(index - 1); visited.set(index - 1) }
                if (currX < width - 1 && !visited.get(index + 1)) { queue.add(index + 1); visited.set(index + 1) }
                if (currY > 0 && !visited.get(index - width)) { queue.add(index - width); visited.set(index - width) }
                if (currY < height - 1 && !visited.get(index + width)) { queue.add(index + width); visited.set(index + width) }
            }
        }
        
        bitmap.setPixels(resultPixels, 0, width, 0, 0, width, height)
    }

    private fun isWithinTolerance(c1: Int, c2: Int, toleranceSq: Float): Boolean {
        if (c1 == c2) return true
        val a1 = (c1 shr 24) and 0xFF
        val r1 = (c1 shr 16) and 0xFF
        val g1 = (c1 shr 8) and 0xFF
        val b1 = c1 and 0xFF
        val a2 = (c2 shr 24) and 0xFF
        val r2 = (c2 shr 16) and 0xFF
        val g2 = (c2 shr 8) and 0xFF
        val b2 = c2 and 0xFF
        val da = a1 - a2
        val dr = r1 - r2
        val dg = g1 - g2
        val db = b1 - b2
        return (da * da + dr * dr + dg * dg + db * db) <= toleranceSq
    }

    suspend fun bakeStrokes(strokes: List<List<TouchPoint>>) {
        if (strokes.all { it.isEmpty() }) return
        for (stroke in strokes) {
            history.add(HistoryItem.Stroke(DrawCommand(stroke, DrawingTool.BRUSH, android.graphics.Color.BLACK, 5f)))
        }
        rebuildCurrentBitmap()
    }
}
