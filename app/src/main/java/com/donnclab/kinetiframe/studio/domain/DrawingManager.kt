package com.donnclab.kinetiframe.studio.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import com.donnclab.kinetiframe.actors.domain.RiggingManager
import com.donnclab.kinetiframe.core.document.BrushSnapshot
import com.donnclab.kinetiframe.core.document.DrawCommand
import com.donnclab.kinetiframe.core.document.DrawingTool
import com.donnclab.kinetiframe.core.document.StrokeBakeReceipts
import com.donnclab.kinetiframe.core.document.StrokeSubmission
import com.donnclab.kinetiframe.core.document.TouchPoint
import com.donnclab.kinetiframe.studio.EraserType
import com.donnclab.kinetiframe.studio.RulerMode
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.currentFrameLayers
import com.donnclab.kinetiframe.studio.drawing.FloodFill
import com.donnclab.kinetiframe.studio.drawing.RulerStrokeConstraintSession
import com.donnclab.kinetiframe.studio.drawing.StrokeHitTest
import com.donnclab.kinetiframe.studio.drawing.snapToRuler
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import com.donnclab.kinetiframe.studio.ui.canvas.toConstraintSpec
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Manager for handling drawing logic, including stroke baking, undo/redo, and ruler application.
 */
class DrawingManager(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val cacheManager: FrameCacheManager,
    private val bakeMutex: Mutex,
    private val frameOperationMutex: Mutex,
    private val getState: () -> StudioState,
    private val updateState: ((StudioState) -> StudioState) -> Unit,
    private val getBaseBitmap: () -> Bitmap?,
    private val setBaseBitmap: (Bitmap?) -> Unit,
    private val activeLayerId: () -> String,
    private val composeFrame: suspend (frameIndex: Int, layerId: String, layerBitmap: Bitmap?) -> Bitmap?,
    private val history: MutableList<HistoryItem>,
    private val redoHistory: MutableList<HistoryItem>,
    private val acquireBitmap: (Int, Int) -> Bitmap,
    private val releaseBitmap: (Bitmap?) -> Unit,
    /** Fired after a layer's frame bitmap settles (stroke bake, undo/redo, ...). Used to drive
     *  "tap a bone, then sketch" auto-binding -- see [RiggingManager.autoBindLayerContent]. */
    private val onFrameBaked: (layerId: String, bitmap: Bitmap) -> Unit = { _, _ -> },
    private val pendingOperations: PendingOperations = PendingOperations(),
) {
    private val maxHistory = 20
    private val selectionHistoryBudget = minOf(24L * 1024 * 1024, Runtime.getRuntime().maxMemory() / 12)
    private val painter = HistoryItemPainter()

    // Raster commands are meaningful only for the layer and frame they were created on. Sharing
    // this session across auto-selected body-part layers rebuilt torso/leg commands into the next
    // body part, then made every bound texture animate at once.
    private var sessionLayerId: String? = null
    private var sessionFrameIndex: Int? = null

    private fun retireStrokeOverlay(seed: Long) {
        updateState { state ->
            state.copy(
                bakedStrokeSeeds =
                StrokeBakeReceipts.acknowledge(state.bakedStrokeSeeds, seed),
            )
        }
    }

    fun bakeStroke(points: List<TouchPoint>, brushSnapshot: BrushSnapshot? = null, targetLayerId: String? = null) {
        if (points.isEmpty()) return
        // Capture identity before suspension: queue delays must never redirect a finished mark.
        val submittedState = getState()
        val submittedConfig = submittedState.tool.activeConfig
        val submission = StrokeSubmission.capture(
            DrawCommand(
                points,
                submittedState.tool.activeTool,
                submittedConfig.color.toArgb(),
                submittedConfig.size,
                submittedConfig.opacity,
                submittedConfig.strength,
                brushSnapshot ?: BrushSnapshot(id = submittedConfig.brushId, seed = points.first().timestamp),
            ),
            submittedState.animation.currentFrame,
            targetLayerId ?: activeLayerId(),
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
                    val eraserPoints = submittedPoints.snapToRuler(drawingState.canvas.rulerGuide)
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
                        if (strokes.any { stroke ->
                                stroke.tool != DrawingTool.ERASER &&
                                    StrokeHitTest.intersects(stroke, eraserPoints, eraserSize)
                            }
                        ) {
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
                    submittedPoints.snapToRuler(drawingState.canvas.rulerGuide)
                }
                val command = submission.command.copy(points = commandPoints)

                val guide = drawingState.canvas.rulerGuide
                if (drawingState.tool.activeTool == DrawingTool.BRUSH && guide?.mode == RulerMode.MIRROR) {
                    val reflection = RulerStrokeConstraintSession(guide.toConstraintSpec())
                    history.add(
                        HistoryItem.StrokeBatch(
                            listOf(command, command.copy(points = submittedPoints.map(reflection::reflect))),
                        ),
                    )
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
        while (history.size > maxHistory || (history.size > 1 && retainedPixelBytes() > selectionHistoryBudget)) {
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
            StrokeSubmission.capture(
                DrawCommand(
                    points,
                    DrawingTool.BRUSH,
                    config.color.toArgb(),
                    config.size,
                    config.opacity,
                    config.strength,
                    BrushSnapshot(id = config.brushId, seed = points.first().timestamp),
                ),
                frameIndex,
                layerId,
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

    private suspend fun bakeToBase(command: DrawCommand) = withContext(dispatcher) {
        bakeMutex.withLock {
            val stage = getState().project.stage
            val width = stage.width
            val height = stage.height
            val bitmap = getBaseBitmap() ?: acquireBitmap(width, height).also { setBaseBitmap(it) }

            val canvas = Canvas(bitmap)
            painter.commands.draw(canvas, command, bitmap)
        }
    }

    private suspend fun applySelectionToBase(edit: HistoryItem.SelectionEdit) = withContext(dispatcher) {
        bakeMutex.withLock {
            val stage = getState().project.stage
            val width = stage.width
            val height = stage.height
            val bitmap = getBaseBitmap() ?: acquireBitmap(width, height).also { setBaseBitmap(it) }
            painter.paintSelectionEdit(Canvas(bitmap), edit)
        }
    }

    private suspend fun applyRasterSelectionToBase(edit: HistoryItem.RasterSelection) = withContext(dispatcher) {
        bakeMutex.withLock {
            val stage = getState().project.stage
            val bitmap = getBaseBitmap() ?: acquireBitmap(stage.width, stage.height).also { setBaseBitmap(it) }
            painter.paintRasterSelection(Canvas(bitmap), edit)
        }
    }

    suspend fun rebuildCurrentBitmap(
        targetLayerId: String = sessionLayerId ?: activeLayerId(),
        targetFrameIndex: Int = sessionFrameIndex ?: getState().animation.currentFrame,
        completedStrokeSeed: Long? = null,
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

            painter.paintAll(Canvas(resultBitmap), history, resultBitmap)

            cacheManager.putFrame(targetLayerId, targetFrameIndex, resultBitmap)
            val compositeBitmap = composeFrame(targetFrameIndex, targetLayerId, resultBitmap)
            withContext(Dispatchers.Main) {
                onFrameBaked(targetLayerId, resultBitmap)
                updateState { state ->
                    val targetLayers = state.currentFrameLayers()
                    val exposures = cacheManager.exposures()
                    val cel = exposures[targetLayerId]?.get(targetFrameIndex)
                    val affectedFrames = if (cel == null) {
                        setOf(targetFrameIndex)
                    } else {
                        exposures[targetLayerId].orEmpty().filterValues { it == cel }.keys
                    }
                    val updatedLayers = targetLayers.map { layer ->
                        if (layer.id == targetLayerId) {
                            layer.copy(populatedFrames = layer.populatedFrames + affectedFrames)
                        } else {
                            layer
                        }
                    }
                    state.copy(
                        currentBitmap = if (state.animation.currentFrame ==
                            targetFrameIndex
                        ) {
                            compositeBitmap
                        } else {
                            state.currentBitmap
                        },
                        bakedStrokeSeeds = StrokeBakeReceipts.acknowledge(state.bakedStrokeSeeds, completedStrokeSeed),
                        layers = state.layers.copy(
                            layers = updatedLayers,
                            frameLayers =
                            state.layers.frameLayers.mapValues { updatedLayers } +
                                (targetFrameIndex to updatedLayers),
                            linkedCels = exposures,
                        ),
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
        updateState { state ->
            state.copy(
                project = state.project.copy(
                    actorPlacements =
                    (state.project.actorPlacements - actorKeys) + (if (undo) edit.beforeActors else edit.afterActors),
                    videoClips =
                    (state.project.videoClips - videoKeys) + (if (undo) edit.beforeVideos else edit.afterVideos),
                    audioClips =
                    (state.project.audioClips - audioKeys) + (if (undo) edit.beforeAudio else edit.afterAudio),
                ),
                scene = state.scene.copy(
                    nodes =
                    (state.scene.nodes - nodeKeys) + (if (undo) edit.beforeNodes else edit.afterNodes),
                ),
            )
        }
    }

    fun clearAllHandwriting() {
        pendingOperations.launch(scope, dispatcher) {
            frameOperationMutex.withLock {
                history.clear()
                redoHistory.clear()
                getBaseBitmap()?.eraseColor(Color.TRANSPARENT)
                rebuildCurrentBitmap()
            }
        }
    }

    suspend fun generateFillPreview(offset: Offset): Bitmap? = withContext(dispatcher) {
        val currentState = getState()
        val stage = getState().project.stage
        val width = stage.width
        val height = stage.height

        // Use a clean bitmap for the preview
        val previewBitmap = acquireBitmap(width, height)
        previewBitmap.eraseColor(Color.TRANSPARENT)

        // Fill boundaries come from the active layer only: its base bitmap plus its stroke history.
        val referenceBitmap = acquireBitmap(width, height)
        referenceBitmap.eraseColor(Color.TRANSPARENT)
        val refCanvas = Canvas(referenceBitmap)
        getBaseBitmap()?.let { refCanvas.drawBitmap(it, 0f, 0f, null) }
        for (item in history) {
            when (item) {
                is HistoryItem.Stroke -> painter.commands.draw(refCanvas, item.command, referenceBitmap)

                is HistoryItem.StrokeBatch -> item.commands.forEach {
                    painter.commands.draw(refCanvas, it, referenceBitmap)
                }

                else -> Unit
            }
        }

        val config = currentState.tool.activeConfig
        FloodFill.fill(
            previewBitmap,
            offset.x.toInt(),
            offset.y.toInt(),
            config.color.toArgb(),
            config.size,
            referenceBitmap,
        )

        releaseBitmap(referenceBitmap)
        previewBitmap
    }

    suspend fun bakeStrokes(strokes: List<List<TouchPoint>>) {
        if (strokes.all { it.isEmpty() }) return
        for (stroke in strokes) {
            history.add(HistoryItem.Stroke(DrawCommand(stroke, DrawingTool.BRUSH, Color.BLACK, 5f)))
        }
        rebuildCurrentBitmap()
    }
}
