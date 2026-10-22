package com.donnclab.kinetiframe.ui.components.canvas

import android.graphics.Bitmap
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInteropFilter
import com.donnclab.kinetiframe.engine.model.DrawingTool
import com.donnclab.kinetiframe.engine.model.DocumentCoordinateTransform
import com.donnclab.kinetiframe.engine.model.StageDimensions
import com.donnclab.kinetiframe.engine.model.TouchPoint
import com.donnclab.kinetiframe.engine.ruler.RulerStrokeConstraintSession
import com.donnclab.kinetiframe.engine.shape.ConstructionShape
import com.donnclab.kinetiframe.engine.shape.ConstructionGesturePolicy
import com.donnclab.kinetiframe.engine.shape.ConstructionShapeSession
import com.donnclab.kinetiframe.models.*
import com.donnclab.kinetiframe.ui.*

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CanvasInputLayer(
    modifier: Modifier = Modifier,
    stage: StageDimensions,
    canvasMatrixValues: FloatArray,
    sourceFrame: Bitmap?,
    /** Raw pixels of the selected layer. Manipulation must never sample the composited frame. */
    manipulationSource: Bitmap? = null,
    manipulationWidth: Float = 0f,
    manipulationStrength: Float = 0f,
    studioMode: StudioMode,
    activeTool: DrawingTool,
    pressureSensitive: Boolean = false,
    rulerGuide: RulerGuide?,
    constructionShape: ConstructionShape? = null,
    constructionPreviewPending: Boolean = false,
    selectionActive: Boolean,
    nodes: Map<String, com.donnclab.kinetiframe.models.BaseNode>,
    displayDensity: Float,
    fontRegistry: com.donnclab.kinetiframe.engine.typography.FontRegistry?,
    selectionTool: SelectionToolType,
    activeStroke: ActiveStroke,
    selectionPath: Path,
    onSelectionRectChanged: (Rect?) -> Unit,
    onTextNodeSelect: (String?) -> Unit,
    onSelectionAreaComplete: (Path?, Rect?, Float, com.donnclab.kinetiframe.engine.typography.FontRegistry?) -> Unit,
    onEraserCursorChange: (Offset?) -> Unit,
    onIntent: (StudioIntent) -> Unit,
    onStrokeSettled: (List<TouchPoint>) -> Unit,
    onStrokeComplete: (List<TouchPoint>) -> Unit,
    onConstructionPreview: (List<List<TouchPoint>>) -> Unit = {},
    onRigJointHit: ((Offset) -> String?)? = null,
    content: @Composable () -> Unit
) {
    
    var lastTapTime by remember { mutableStateOf(0L) }
    var lastTapPoint by remember { mutableStateOf(Offset.Zero) }
    // Consumes the ACTION_UP after a direct node pick so it cannot begin a lasso.
    var consumedTextSelection by remember { mutableStateOf(false) }
    val activePointerSession = remember { ActivePointerSession() }
    var currentToolType by remember { mutableIntStateOf(MotionEvent.TOOL_TYPE_UNKNOWN) }
    var rulerSession by remember { mutableStateOf<RulerStrokeConstraintSession?>(null) }
    var constructionSession by remember { mutableStateOf<ConstructionShapeSession?>(null) }
    var rigGestureCancelled by remember { mutableStateOf(false) }
    var draggedBone by remember { mutableStateOf<String?>(null) }
    var rigDownScreen by remember { mutableStateOf(Offset.Zero) }
    
    val coordinateTransform = remember(stage, canvasMatrixValues) {
        DocumentCoordinateTransform(stage, canvasMatrixValues)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInteropFilter { event ->
                val rawAction = event.actionMasked
                // Cancellation must be handled even when Android has already removed the pointer.
                val cancelledUp = (rawAction == MotionEvent.ACTION_UP || rawAction == MotionEvent.ACTION_POINTER_UP) &&
                    event.flags and MotionEvent.FLAG_CANCELED != 0 &&
                    activePointerSession.isActive(event.getPointerId(event.actionIndex))
                if (rawAction == MotionEvent.ACTION_CANCEL || cancelledUp) {
                    activeStroke.reset()
                    rulerSession = null
                    constructionSession = null
                    onConstructionPreview(emptyList())
                    selectionPath.reset()
                    onSelectionRectChanged(null)
                    onEraserCursorChange(null)
                    if (draggedBone != null) onIntent(StudioIntent.Actor.CancelDrag)
                    draggedBone = null
                    consumedTextSelection = false
                    activePointerSession.clear()
                    currentToolType = MotionEvent.TOOL_TYPE_UNKNOWN
                    return@pointerInteropFilter true
                }
                val actionTool = event.getToolType(event.actionIndex)
                val stylusContact = actionTool == MotionEvent.TOOL_TYPE_STYLUS || actionTool == MotionEvent.TOOL_TYPE_ERASER
                val stylusTakesOver = studioMode == StudioMode.RASTER_DRAWING &&
                    rawAction == MotionEvent.ACTION_POINTER_DOWN && stylusContact &&
                    currentToolType != MotionEvent.TOOL_TYPE_STYLUS && currentToolType != MotionEvent.TOOL_TYPE_ERASER
                if (stylusTakesOver) {
                    // Discard the provisional finger/palm mark rather than committing it.
                    activeStroke.reset()
                    rulerSession = null
                    activePointerSession.begin(event.getPointerId(event.actionIndex), event.eventTime)
                    currentToolType = actionTool
                }
                if (rawAction == MotionEvent.ACTION_DOWN) {
                    rigGestureCancelled = false
                    val pointerId = event.getPointerId(event.actionIndex)
                    activePointerSession.begin(pointerId, event.eventTime)
                    currentToolType = event.getToolType(event.actionIndex)
                }

                val action = if (stylusTakesOver && rawAction == MotionEvent.ACTION_POINTER_DOWN &&
                    activePointerSession.isActive(event.getPointerId(event.actionIndex))) MotionEvent.ACTION_DOWN else rawAction

                if (studioMode != StudioMode.RASTER_DRAWING &&
                    (event.pointerCount > 1 || action == MotionEvent.ACTION_CANCEL)) {
                    rigGestureCancelled = true
                    if (draggedBone != null) onIntent(StudioIntent.Actor.CancelDrag)
                    draggedBone = null
                    selectionPath.reset()
                    activeStroke.invalidate()
                }
                if (studioMode != StudioMode.RASTER_DRAWING && rigGestureCancelled) return@pointerInteropFilter true

                val activePointerIndex = when (action) {
                    MotionEvent.ACTION_DOWN -> event.actionIndex
                    else -> event.findPointerIndex(activePointerSession.pointerId)
                }

                if (activePointerIndex < 0) return@pointerInteropFilter true

                // Palm Rejection: If a stylus is active, ignore all finger pointers for drawing
                val toolType = event.getToolType(activePointerIndex)
                if (currentToolType == MotionEvent.TOOL_TYPE_STYLUS && toolType == MotionEvent.TOOL_TYPE_FINGER) {
                    return@pointerInteropFilter false
                }
                
                if (action == MotionEvent.ACTION_POINTER_UP &&
                    activePointerSession.shouldIgnorePointerUp(event.getPointerId(event.actionIndex))
                ) {
                    return@pointerInteropFilter true
                }

                
                fun mapToDocument(x: Float, y: Float): Offset? =
                    coordinateTransform.toDocument(Offset(x, y))

                val screenX = event.getX(activePointerIndex)
                val screenY = event.getY(activePointerIndex)
                val mappedPoint = mapToDocument(screenX, screenY) ?: return@pointerInteropFilter false
                val mappedX = mappedPoint.x
                val mappedY = mappedPoint.y
                val isWithinDocument = coordinateTransform.contains(mappedPoint)

                // Do not start a document edit in the viewport's letterbox area.  A
                // stroke that leaves the sheet remains active, but samples outside it
                // are discarded; releasing outside still commits its in-bounds part.
                if (!isWithinDocument && action == MotionEvent.ACTION_DOWN && activeTool != DrawingTool.LASSO) {
                    return@pointerInteropFilter false
                }

                if (!isWithinDocument && action == MotionEvent.ACTION_MOVE) {
                    if (activeTool == DrawingTool.ERASER) onEraserCursorChange(null)
                    return@pointerInteropFilter true
                }

                if (studioMode != StudioMode.RASTER_DRAWING && activeTool == DrawingTool.BONE) {
                    if (action == MotionEvent.ACTION_UP && isWithinDocument) {
                        onIntent(StudioIntent.Rigging.AddBoneAt(mappedX, mappedY))
                    }
                    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        activePointerSession.end(event.getPointerId(activePointerIndex))
                    }
                    return@pointerInteropFilter true
                }

                val isRiggingTool = activeTool == DrawingTool.RIG_SELECT || activeTool == DrawingTool.BONE
                
                if (studioMode != StudioMode.RASTER_DRAWING && isRiggingTool) {
                    when (action) {
                        MotionEvent.ACTION_DOWN -> {
                            draggedBone = onRigJointHit?.invoke(mappedPoint)
                            rigDownScreen = Offset(screenX, screenY)
                            onIntent(StudioIntent.Rigging.SelectBone(draggedBone?.removePrefix("origin:")?.removePrefix("artwork:")?.removePrefix("attachment:")))
                            draggedBone?.takeIf { it.startsWith("artwork:") }?.let {
                                onIntent(StudioIntent.Actor.Drag(it, mappedPoint))
                            }
                        }
                        MotionEvent.ACTION_MOVE -> draggedBone?.let {
                            if ((Offset(screenX, screenY) - rigDownScreen).getDistance() > 4f * displayDensity) {
                                onIntent(StudioIntent.Actor.Drag(it, mappedPoint))
                            }
                        }
                        MotionEvent.ACTION_UP -> {
                            onIntent(StudioIntent.Actor.CommitDrag)
                            draggedBone = null
                            activePointerSession.end(event.getPointerId(activePointerIndex))
                        }
                    }
                    return@pointerInteropFilter true
                }

                // Once a selection exists, the lasso tool is in transform mode. It
                // must never fall through to the drawing handler beneath the box.
                // The transform overlay above this layer owns taps and handle drags.
                if (activeTool == DrawingTool.LASSO && selectionActive) {
                    activeStroke.reset()
                    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        activePointerSession.end(event.getPointerId(activePointerIndex))
                    }
                    return@pointerInteropFilter true
                }

                if (activeTool == DrawingTool.COLOR_FILL) {
                    when (action) {
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                            val duration = event.eventTime - activePointerSession.startTime
                            if (duration < 250) {
                                onIntent(StudioIntent.Fill.SetPendingTap(mappedPoint))
                                activePointerSession.end(event.getPointerId(activePointerIndex))
                                return@pointerInteropFilter true
                            }
                        }
                    }
                    return@pointerInteropFilter true
                }

                if (activeTool == DrawingTool.TEXT) {
                    when (action) {
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                            val duration = event.eventTime - activePointerSession.startTime
                            if (duration < 200) {
                                val hitText = nodes.values.filterIsInstance<TextNode>().lastOrNull { node ->
                                    textHit(node, mappedPoint, displayDensity, fontRegistry)
                                }
                                if (hitText != null) onIntent(StudioIntent.Text.TriggerEditDialog(hitText.id))
                                else onIntent(StudioIntent.Text.TriggerAddTextDialog(mappedX, mappedY))
                                activePointerSession.end(event.getPointerId(activePointerIndex))
                                return@pointerInteropFilter true
                            }
                        }
                    }
                }

                if ((activeTool == DrawingTool.LASSO || activeTool == DrawingTool.LASSO_CUT) && !selectionActive) {
                    when (action) {
                        MotionEvent.ACTION_DOWN -> {
                            selectionPath.reset()
                            if (selectionTool == SelectionToolType.LASSO) {
                                selectionPath.moveTo(mappedX, mappedY)
                            } else {
                                onSelectionRectChanged(Rect(mappedX, mappedY, mappedX, mappedY))
                            }
                            activePointerSession.startX = mappedX
                            activePointerSession.startY = mappedY
                            activeStroke.invalidate()
                            return@pointerInteropFilter true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (selectionTool == SelectionToolType.LASSO) {
                                selectionPath.lineTo(mappedX, mappedY)
                            } else {
                                val startX = activePointerSession.startX
                                val startY = activePointerSession.startY
                                onSelectionRectChanged(Rect(minOf(startX, mappedX), minOf(startY, mappedY), maxOf(startX, mappedX), maxOf(startY, mappedY)))
                            }
                            activeStroke.invalidate()
                            return@pointerInteropFilter true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                            if (activeTool == DrawingTool.LASSO_CUT) {
                                selectionPath.close()
                                sourceFrame?.let { frame ->
                                    onIntent(
                                        StudioIntent.Rigging.PreparePivotPlacement(
                                            sourceFrame = frame,
                                            lassoPath = Path().apply { addPath(selectionPath) }
                                        )
                                    )
                                }
                                selectionPath.reset()
                                onSelectionRectChanged(null)
                                activePointerSession.end(event.getPointerId(activePointerIndex))
                                return@pointerInteropFilter true
                            }
                            if (consumedTextSelection) {
                                consumedTextSelection = false
                                activePointerSession.end(event.getPointerId(activePointerIndex))
                                return@pointerInteropFilter true
                            }
                            val distance = (mappedPoint - Offset(activePointerSession.startX, activePointerSession.startY)).getDistance()
                            if (distance < 2f && selectionPath.getBounds().width < 3f && selectionPath.getBounds().height < 3f) {
                                onSelectionAreaComplete(null, Rect(mappedX - .5f, mappedY - .5f, mappedX + .5f, mappedY + .5f), displayDensity, fontRegistry)
                            } else if (selectionTool == SelectionToolType.LASSO) {
                                selectionPath.close()
                                val completedPath = Path().apply { addPath(selectionPath) }
                                onSelectionAreaComplete(completedPath, null, displayDensity, fontRegistry)
                            } else {
                                val startX = activePointerSession.startX
                                val startY = activePointerSession.startY
                                val rect = Rect(
                                    minOf(startX, mappedX), minOf(startY, mappedY),
                                    maxOf(startX, mappedX), maxOf(startY, mappedY)
                                )
                                onSelectionAreaComplete(null, rect, displayDensity, fontRegistry)
                            }
                            selectionPath.reset()
                            onSelectionRectChanged(null)
                            activePointerSession.end(event.getPointerId(activePointerIndex))
                            return@pointerInteropFilter true
                        }
                    }
                }

                val canStartConstruction = ConstructionGesturePolicy.canHandle(activeTool, constructionShape, constructionPreviewPending)
                if (constructionShape != null && constructionPreviewPending && constructionSession == null) {
                    // A pending construction preview owns the canvas until Check or X;
                    // never fall through to freehand Brush input.
                    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        activePointerSession.end(event.getPointerId(activePointerIndex))
                    }
                    return@pointerInteropFilter true
                }
                if (canStartConstruction || constructionSession != null) {
                    when (action) {
                        MotionEvent.ACTION_DOWN -> {
                            val session = ConstructionShapeSession(requireNotNull(constructionShape))
                            session.begin(TouchPoint(mappedX, mappedY, event.getPressure(activePointerIndex), timestamp = event.eventTime))
                            constructionSession = session
                            return@pointerInteropFilter true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            constructionSession?.let { session ->
                                val sample = TouchPoint(mappedX, mappedY, event.getPressure(activePointerIndex), timestamp = event.eventTime)
                                session.update(sample)
                                session.preview()?.let(onConstructionPreview)
                            }
                            return@pointerInteropFilter true
                        }
                        MotionEvent.ACTION_UP -> {
                            constructionSession?.complete()?.let(onConstructionPreview)
                            constructionSession = null
                            activePointerSession.end(event.getPointerId(activePointerIndex))
                            return@pointerInteropFilter true
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            constructionSession = null
                            onConstructionPreview(emptyList())
                            activePointerSession.clear()
                            return@pointerInteropFilter true
                        }
                    }
                }

                if (activeTool == DrawingTool.ERASER) {
                    when (action) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> onEraserCursorChange(Offset(screenX, screenY))
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> onEraserCursorChange(null)
                    }
                }

                // If lasso or text is active, don't fall through to general stroke handling
                if (activeTool == DrawingTool.LASSO || activeTool == DrawingTool.LASSO_CUT || activeTool == DrawingTool.TEXT) {
                    return@pointerInteropFilter true
                }

                when (action) {
                    MotionEvent.ACTION_DOWN -> {
                        val currentTime = System.currentTimeMillis()
                        val tapPoint = Offset(screenX, screenY)
                        
                        // Double tap detection
                        if (currentTime - lastTapTime < 300L && (tapPoint - lastTapPoint).getDistance() < 50f) {
                            val hitText = nodes.values.filterIsInstance<TextNode>().lastOrNull { node ->
                                textHit(node, mappedPoint, displayDensity, fontRegistry)
                            }
                            if (hitText != null) {
                                onIntent(StudioIntent.Text.TriggerEditDialog(hitText.id))
                                lastTapTime = 0L // Reset to prevent triple tap as double double tap
                                return@pointerInteropFilter true
                            }
                        }
                        
                        lastTapTime = currentTime
                        lastTapPoint = tapPoint

                        activeStroke.reset()
                        activeStroke.configureInput(displayDensity, pressureSensitive)
                        activeStroke.isStylusContact = currentToolType == MotionEvent.TOOL_TYPE_STYLUS ||
                            currentToolType == MotionEvent.TOOL_TYPE_ERASER
                        if ((activeTool == DrawingTool.BLUR || activeTool == DrawingTool.SMUDGE) && manipulationSource != null) {
                            activeStroke.beginManipulation(
                                source = manipulationSource,
                                tool = activeTool,
                                baseWidth = manipulationWidth,
                                strength = manipulationStrength
                            )
                        }
                        rulerSession = if (activeTool == DrawingTool.BRUSH && rulerGuide != null) {
                            RulerStrokeConstraintSession(rulerGuide.toConstraintSpec())
                        } else {
                            null
                        }
                        val point = TouchPoint(
                            mappedX,
                            mappedY,
                            event.getPressure(activePointerIndex),
                            event.getAxisValue(MotionEvent.AXIS_TILT, activePointerIndex),
                            event.getAxisValue(MotionEvent.AXIS_ORIENTATION, activePointerIndex),
                            velocity = 0f,
                            timestamp = event.eventTime
                        )
                        activeStroke.appendSample(
                            sample = point,
                            screenSample = TouchPoint(screenX, screenY, event.getPressure(activePointerIndex), velocity = 0f, timestamp = event.eventTime),
                            constraint = rulerSession?.let { session -> session::constrain }
                        )?.also { constrained ->
                            if (rulerGuide?.mode == RulerMode.MIRROR) activeStroke.appendMirroredSample(requireNotNull(rulerSession).reflect(constrained))
                        }
                        activeStroke.invalidate()
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val historySize = event.historySize
                        val previousPointCount = activeStroke.points.size
                        for (i in 0 until historySize) {
                            val historical = mapToDocument(event.getHistoricalX(activePointerIndex, i), event.getHistoricalY(activePointerIndex, i))
                            if (historical != null && coordinateTransform.contains(historical)) {
                                activeStroke.appendSample(
                                    sample = TouchPoint(
                                        historical.x,
                                        historical.y,
                                        event.getHistoricalPressure(activePointerIndex, i),
                                        event.getHistoricalAxisValue(MotionEvent.AXIS_TILT, activePointerIndex, i),
                                        event.getHistoricalAxisValue(MotionEvent.AXIS_ORIENTATION, activePointerIndex, i),
                                        velocity = 0f,
                                        timestamp = event.getHistoricalEventTime(i)
                                    ),
                                    screenSample = TouchPoint(
                                        event.getHistoricalX(activePointerIndex, i),
                                        event.getHistoricalY(activePointerIndex, i),
                                        event.getHistoricalPressure(activePointerIndex, i),
                                        velocity = 0f,
                                        timestamp = event.getHistoricalEventTime(i)
                                    ),
                                    constraint = rulerSession?.let { session -> session::constrain }
                                )?.also { constrained ->
                                    if (rulerGuide?.mode == RulerMode.MIRROR) activeStroke.appendMirroredSample(requireNotNull(rulerSession).reflect(constrained))
                                }
                            }
                        }
                        val point = TouchPoint(
                            mappedX,
                            mappedY,
                            event.getPressure(activePointerIndex),
                            event.getAxisValue(MotionEvent.AXIS_TILT, activePointerIndex),
                            event.getAxisValue(MotionEvent.AXIS_ORIENTATION, activePointerIndex),
                            velocity = 0f,
                            timestamp = event.eventTime
                        )
                        activeStroke.appendSample(
                            sample = point,
                            screenSample = TouchPoint(screenX, screenY, event.getPressure(activePointerIndex), velocity = 0f, timestamp = event.eventTime),
                            constraint = rulerSession?.let { session -> session::constrain }
                        )?.also { constrained ->
                            if (rulerGuide?.mode == RulerMode.MIRROR) activeStroke.appendMirroredSample(requireNotNull(rulerSession).reflect(constrained))
                        }

                        // Raw prediction bypassed conditioning and made a held nib visibly dance.
                        // Render the latest conditioned contact; prediction is not committed ink.
                        activeStroke.predictedTouchPoint = null
                        activeStroke.predictedMirroredTouchPoint = null

                        if (activeStroke.points.size != previousPointCount) activeStroke.invalidate()
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                        val last = activeStroke.points.lastOrNull()
                        val manipulationLift = (activeTool == DrawingTool.SMUDGE || activeTool == DrawingTool.BLUR) &&
                            last?.timestamp != event.eventTime
                        if (isWithinDocument && (last == null || last.x != mappedX || last.y != mappedY || manipulationLift)) {
                            activeStroke.flushSample(
                                TouchPoint(
                                    mappedX,
                                    mappedY,
                                    event.getPressure(activePointerIndex),
                                    event.getAxisValue(MotionEvent.AXIS_TILT, activePointerIndex),
                                    event.getAxisValue(MotionEvent.AXIS_ORIENTATION, activePointerIndex),
                                    velocity = 0f,
                                    timestamp = event.eventTime
                                ),
                                constraint = rulerSession?.let { session -> session::constrain },
                                screenSample = TouchPoint(screenX, screenY, timestamp = event.eventTime)
                            ).also { constrained ->
                                if (rulerGuide?.mode == RulerMode.MIRROR) activeStroke.appendMirroredSample(requireNotNull(rulerSession).reflect(constrained))
                            }
                        }
                        // Commit exactly the stabilized points the live preview showed. Simplifying
                        // the stroke on release would visibly change its shape and pressure at the
                        // moment it becomes a bitmap.
                        activeStroke.predictedTouchPoint = null
                        val completedPoints = ArrayList(activeStroke.points)
                        onStrokeSettled(completedPoints)
                        onStrokeComplete(completedPoints)
                        activeStroke.reset()
                        rulerSession = null
                        activePointerSession.end(event.getPointerId(activePointerIndex))
                        currentToolType = MotionEvent.TOOL_TYPE_UNKNOWN
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        activeStroke.reset()
                        rulerSession = null
                        activePointerSession.clear()
                        currentToolType = MotionEvent.TOOL_TYPE_UNKNOWN
                        true
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        true
                    }
                    else -> false
                }
            }
    ) {
        content()
    }
}
