package com.donnclab.kinetiframe.studio.ui.canvas

import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.MotionEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.core.document.BaseNode
import com.donnclab.kinetiframe.core.document.BrushId
import com.donnclab.kinetiframe.core.document.BrushSnapshot
import com.donnclab.kinetiframe.core.document.DrawingTool
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.core.document.TextNode
import com.donnclab.kinetiframe.core.document.TouchPoint
import com.donnclab.kinetiframe.studio.EraserType
import com.donnclab.kinetiframe.studio.SelectedContent
import com.donnclab.kinetiframe.studio.SelectionToolType
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.currentFrameLayers
import com.donnclab.kinetiframe.studio.drawing.ConstructionShape
import com.donnclab.kinetiframe.studio.drawing.FontRegistry
import com.donnclab.kinetiframe.studio.drawing.MultilineTextRenderer
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * A Compose component that renders a raster background from a FrameCacheManager
 * and overlays an active vector stroke.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BufferedDrawingCanvas(
    studioState: StudioState,
    currentFrameBitmap: Bitmap?,
    onStrokeComplete: (List<TouchPoint>, BrushSnapshot) -> Unit,
    modifier: Modifier = Modifier,
    strokeColor: Int = android.graphics.Color.BLACK,
    strokeWidth: Float = 5f,
    strokeAlpha: Float = 1f,
    strength: Float = 0.5f,
    brushId: BrushId = BrushId.PEN,
    activeTool: DrawingTool = DrawingTool.BRUSH,
    constructionShape: ConstructionShape? = null,
    constructionPreviewPending: Boolean = false,
    eraserType: EraserType = EraserType.AREA,
    eraserCursor: Offset? = null,
    selectionTool: SelectionToolType = SelectionToolType.LASSO,
    selectionActive: Boolean = false,
    selectionBounds: Rect? = null,
    selectedContent: SelectedContent? = null,
    selectionTransform: Matrix = Matrix(),
    onSelectionAreaComplete: (
        Path?,
        Rect?,
        Float,
        com.donnclab.kinetiframe.studio.drawing.FontRegistry?,
    ) -> Unit = { _, _, _, _ -> },
    onSelectionTransformChanged: (Matrix) -> Unit = {},
    onEraserCursorChange: (Offset?) -> Unit = {},
    onionSkinEnabled: Boolean = false,
    onionSkinFramesBefore: Int = 1,
    onionSkinFramesAfter: Int = 1,
    onionSkinColorBefore: Color = Color(0xFF92003B),
    onionSkinColorAfter: Color = Color(0xFF00C853),
    onionSkinOpacity: Float = 0.5f,
    cacheManager: FrameCacheManager? = null,
    activeLayerId: String = "main",
    nodes: Map<String, com.donnclab.kinetiframe.core.document.BaseNode> = emptyMap(),
    onTextNodeUpdate: (TextNode) -> Unit = {},
    onTextNodeSelect: (String?) -> Unit = {},
    onIntent: (StudioIntent) -> Unit = {},
    onConstructionPreview: (List<List<TouchPoint>>) -> Unit = {},
    /** The project's persistent background image, already decoded -- see [CanvasDrawingLayer]'s
     *  doc comment on its own parameter of the same name for the full rationale. */
    backgroundBitmap: Bitmap? = null,
    /** The video reference track's current-frame bitmap and its track-wide opacity -- see
     *  [CanvasDrawingLayer]'s doc comment on its own parameters of the same name. */
    videoFrameBitmap: Bitmap? = null,
    videoOpacity: Float = 0.5f,
    externalSelectionOverlay: Boolean = false,
    sharedFontRegistry: FontRegistry? = null,
    referenceUnderlay: ((android.graphics.Canvas) -> Unit)? = null,
    paperDecoration: Boolean = true,
) {
    val context = LocalContext.current
    val displayDensity = LocalDensity.current.density
    val fontRegistry = sharedFontRegistry ?: remember(context) { FontRegistry(context) }

    val paint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
        }
    }
    val pressurePaint = remember { android.graphics.Paint(paint) }

    val pastTint = remember(onionSkinColorBefore) { ColorFilter.tint(onionSkinColorBefore, BlendMode.SrcIn) }
    val futureTint = remember(onionSkinColorAfter) { ColorFilter.tint(onionSkinColorAfter, BlendMode.SrcIn) }

    var onionSnapshot by remember { mutableStateOf(OnionSnapshot(-1, "")) }
    val onionMatchesFrame = onionSkinEnabled &&
        onionSnapshot.frameIndex == studioState.animation.currentFrame &&
        onionSnapshot.layerId == activeLayerId
    val pastBitmaps = if (onionMatchesFrame) onionSnapshot.past else emptyList()
    val futureBitmaps = if (onionMatchesFrame) onionSnapshot.future else emptyList()

    val documentWidth = studioState.project.stage.width
    val documentHeight = studioState.project.stage.height

    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var transformHudVersion by remember { mutableIntStateOf(0) }

    val canvasMatrix = remember(studioState.canvas.matrixValues) {
        android.graphics.Matrix().apply { setValues(studioState.canvas.matrixValues) }
    }

    val scope = rememberCoroutineScope()

    val selectionPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
            color = android.graphics.Color.parseColor("#4285F4")
            this.strokeWidth = 2f
        }
    }

    SideEffect {
        if (activeTool == DrawingTool.ERASER && eraserType == EraserType.AREA) {
            paint.color = android.graphics.Color.TRANSPARENT
            paint.alpha = 0
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        } else {
            paint.color = strokeColor
            paint.alpha = (strokeAlpha * 255).toInt()
            paint.xfermode = null
        }
        paint.strokeWidth = strokeWidth
        pressurePaint.set(paint)
    }

    LaunchedEffect(
        studioState.animation.currentFrame,
        studioState.animation.totalFrames,
        currentFrameBitmap,
        onionSkinEnabled,
        onionSkinFramesBefore,
        onionSkinFramesAfter,
        cacheManager,
        activeLayerId,
    ) {
        if (onionSkinEnabled && cacheManager != null) {
            val past = mutableListOf<OnionFrame>()
            for (i in 1..onionSkinFramesBefore) {
                val idx = studioState.animation.currentFrame - i
                if (idx >= 0) cacheManager.getFrame(activeLayerId, idx)?.let { past.add(OnionFrame(it, i)) }
            }
            val future = mutableListOf<OnionFrame>()
            for (i in 1..onionSkinFramesAfter) {
                val idx = studioState.animation.currentFrame + i
                if (idx <
                    studioState.animation.totalFrames
                ) {
                    cacheManager.getFrame(activeLayerId, idx)?.let { future.add(OnionFrame(it, i)) }
                }
            }
            onionSnapshot = OnionSnapshot(studioState.animation.currentFrame, activeLayerId, past, future)
        } else {
            onionSnapshot = OnionSnapshot(-1, "")
        }
    }

    val activeStroke = remember { ActiveStroke() }
    // Keep the composited frame for display, but obtain the selected layer separately for pixel
    // manipulation. Sampling currentFrameBitmap here would bake neighbouring layers into it.
    var activeLayerBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var manipulationLayers by remember { mutableStateOf<List<Pair<Layer, Bitmap>>>(emptyList()) }
    val frameLayers = studioState.currentFrameLayers()
    LaunchedEffect(
        cacheManager,
        activeLayerId,
        studioState.animation.currentFrame,
        currentFrameBitmap,
        frameLayers,
        activeTool,
    ) {
        activeLayerBitmap = null
        manipulationLayers = emptyList()
        if (activeTool == DrawingTool.SMUDGE || activeTool == DrawingTool.BLUR) {
            val loaded = frameLayers.sortedBy { it.zIndex }.mapNotNull { layer ->
                cacheManager?.getFrame(layer.id, studioState.animation.currentFrame)?.let { layer to it }
            }
            manipulationLayers = loaded
            activeLayerBitmap =
                loaded.firstOrNull { it.first.id == activeLayerId && !it.first.isLocked && it.first.isVisible }?.second
        }
    }
    val selectionPath = remember { Path() }
    var selectionRect by remember { mutableStateOf<Rect?>(null) }

    // Use a stable state holder for selection transforms to avoid parent recomposition.
    val selectionTransformState = remember(selectionActive, selectionBounds) {
        SelectionTransformState(selectionTransform, studioState.selection.pivotOffset)
    }

    // Keep the stable state in sync with the StudioState (e.g. for Reset logic)
    LaunchedEffect(selectionTransform, studioState.selection.pivotOffset) {
        selectionTransformState.matrix = selectionTransform
        selectionTransformState.pivotOffset = studioState.selection.pivotOffset
    }

    // Explicit receipts survive conflated bakes and unrelated bitmap refreshes. Drawing also
    // checks them immediately, before this effect, so translucent ink never deposits twice.
    LaunchedEffect(studioState.bakedStrokeSeeds) {
        activeStroke.acknowledgeSettledStrokes(studioState.bakedStrokeSeeds)
    }
    LaunchedEffect(studioState.animation.currentFrame, activeLayerId) {
        activeStroke.reset()
        activeStroke.clearSettledStrokes()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                viewportSize = it
                onIntent(StudioIntent.View.SetViewportSize(it.width, it.height))
            }
            .pointerInput(Unit) {
                workspaceGestures(
                    touchNavigationEnabled = true,
                    onNavigation = { activeStroke.reset() },
                    onTransform = { pan, zoom, rotation, centre ->
                        transformHudVersion++
                        onIntent(StudioIntent.View.UpdateTransform(pan, zoom, rotation, centre))
                    },
                )
            },
    ) {
        CanvasInputLayer(
            stage = studioState.project.stage,
            canvasMatrixValues = studioState.canvas.matrixValues,
            sourceFrame = currentFrameBitmap,
            manipulationSource = activeLayerBitmap,
            manipulationWidth = strokeWidth,
            manipulationStrength = strength,
            studioMode = studioState.scene.studioMode,
            activeTool = activeTool,
            pressureSensitive =
            activeTool == DrawingTool.BRUSH && brushId != com.donnclab.kinetiframe.core.document.BrushId.PEN,
            rulerGuide = studioState.canvas.rulerGuide,
            constructionShape = constructionShape,
            constructionPreviewPending = constructionPreviewPending,
            selectionActive = selectionActive,
            nodes = nodes,
            displayDensity = displayDensity,
            fontRegistry = fontRegistry,
            selectionTool = selectionTool,
            activeStroke = activeStroke,
            selectionPath = selectionPath,
            onSelectionRectChanged = { selectionRect = it },
            onTextNodeSelect = onTextNodeSelect,
            onSelectionAreaComplete = onSelectionAreaComplete,
            onEraserCursorChange = onEraserCursorChange,
            onIntent = onIntent,
            onStrokeSettled = { points ->
                activeStroke.settle(
                    points = points,
                    tool = activeTool,
                    eraserType = eraserType,
                    width = strokeWidth,
                    strength = strength,
                    paint = pressurePaint,
                    sourceBitmap = currentFrameBitmap,
                    brush = BrushSnapshot(brushId, seed = points.firstOrNull()?.timestamp ?: 0L),
                )
            },
            onStrokeComplete = { points ->
                onStrokeComplete(points, BrushSnapshot(brushId, seed = points.firstOrNull()?.timestamp ?: 0L))
            },
            onConstructionPreview = onConstructionPreview,
        ) {
            CanvasDrawingLayer(
                paperDecoration = paperDecoration,
                modifier = Modifier.fillMaxSize(),
                docWidth = documentWidth,
                docHeight = documentHeight,
                canvasMatrix = canvasMatrix,
                backgroundBitmap = backgroundBitmap,
                referenceUnderlay = referenceUnderlay,
                videoFrameBitmap = videoFrameBitmap,
                videoOpacity = videoOpacity,
                onionSkinEnabled = onionSkinEnabled,
                pastBitmaps = pastBitmaps,
                futureBitmaps = futureBitmaps,
                onionSkinOpacity = onionSkinOpacity,
                pastTint = pastTint,
                futureTint = futureTint,
                currentFrameBitmap = currentFrameBitmap,
                nodes = nodes,
                fontRegistry = fontRegistry,
                displayDensity = displayDensity,
                activeStroke = activeStroke,
                activeTool = activeTool,
                eraserType = eraserType,
                strokeWidth = strokeWidth,
                strokeAlpha = strokeAlpha,
                strength = strength,
                brush = BrushSnapshot(brushId, seed = activeStroke.points.firstOrNull()?.timestamp ?: 0L),
                pressurePaint = pressurePaint,
                selectionPath = selectionPath,
                selectionPaint = selectionPaint,
                selectionRect = selectionRect,
                useSaveLayer = activeTool == DrawingTool.ERASER,
                fillPreviewBitmap = studioState.fill.previewBitmap,
                fillPreviewActive = studioState.fill.previewActive,
                manipulationLayers = manipulationLayers,
                activeLayerId = activeLayerId,
                bakedStrokeSeeds = studioState.bakedStrokeSeeds,
            )
        }

        if (selectionActive && selectionBounds != null && !externalSelectionOverlay) {
            SelectionTransformBox(
                selectedContent = selectedContent,
                selectionBounds = selectionBounds,
                transformState = selectionTransformState,
                fontRegistry = fontRegistry,
                displayDensity = displayDensity,
                canvasMatrixValues = studioState.canvas.matrixValues,
                docWidth = documentWidth,
                docHeight = documentHeight,
                viewportWidth = viewportSize.width,
                viewportHeight = viewportSize.height,
                onDragEnd = {
                    onSelectionTransformChanged(selectionTransformState.matrix)
                    onIntent(StudioIntent.Selection.UpdatePivot(selectionTransformState.pivotOffset))
                },
                onOutsideSelectionTap = {
                    onIntent(StudioIntent.Selection.SetActive(false))
                },
                activeStroke = activeStroke,
                lockAspectRatio = studioState.selection.lockAspectRatio,
                skewMode = studioState.selection.skewMode,
                interactionMode = studioState.selection.interactionMode,
                onIntent = onIntent,
            )
        }

        // Eraser Cursor Overlay
        eraserCursor?.let { cursor ->
            if (activeTool == DrawingTool.ERASER) {
                val docWidth = studioState.project.stage.width
                val docHeight = studioState.project.stage.height
                val fitScale =
                    minOf(viewportSize.width.toFloat() / docWidth, viewportSize.height.toFloat() / docHeight).takeIf {
                        it >
                            0
                    }
                        ?: 1f

                EraserCursorOverlay(
                    cursor = cursor,
                    strokeWidth = strokeWidth,
                    fitScale = fitScale,
                )
            }
        }

        var overlayVisible by remember { mutableStateOf(false) }

        // Only user pan/zoom/rotation gestures trigger the HUD.  Frame loading and
        // viewport layout must never make the transform readout flash.
        LaunchedEffect(transformHudVersion) {
            if (transformHudVersion == 0) return@LaunchedEffect
            overlayVisible = true
            kotlinx.coroutines.delay(2000L)
            overlayVisible = false
        }

        TransformationOverlay(
            zoom = studioState.canvas.zoom,
            offset = studioState.canvas.pan,
            rotation = studioState.canvas.rotation,
            visible = overlayVisible,
        )
    }
}
