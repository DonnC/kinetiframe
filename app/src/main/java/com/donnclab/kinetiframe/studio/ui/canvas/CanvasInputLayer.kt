package com.donnclab.kinetiframe.studio.ui.canvas

import android.graphics.Bitmap
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInteropFilter
import com.donnclab.kinetiframe.core.document.BaseNode
import com.donnclab.kinetiframe.core.document.DrawingTool
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.core.document.TextNode
import com.donnclab.kinetiframe.core.document.TouchPoint
import com.donnclab.kinetiframe.studio.RulerGuide
import com.donnclab.kinetiframe.studio.SelectionToolType
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.drawing.ConstructionGesturePolicy
import com.donnclab.kinetiframe.studio.drawing.ConstructionShape
import com.donnclab.kinetiframe.studio.drawing.ConstructionShapeSession
import com.donnclab.kinetiframe.studio.drawing.FontRegistry

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
    nodes: Map<String, BaseNode>,
    displayDensity: Float,
    fontRegistry: FontRegistry?,
    selectionTool: SelectionToolType,
    activeStroke: ActiveStroke,
    selectionPath: Path,
    onSelectionRectChanged: (Rect?) -> Unit,
    onTextNodeSelect: (String?) -> Unit,
    onSelectionAreaComplete: (Path?, Rect?, Float, FontRegistry?) -> Unit,
    onEraserCursorChange: (Offset?) -> Unit,
    onIntent: (StudioIntent) -> Unit,
    onStrokeSettled: (List<TouchPoint>) -> Unit,
    onStrokeComplete: (List<TouchPoint>) -> Unit,
    onConstructionPreview: (List<List<TouchPoint>>) -> Unit = {},
    onRigJointHit: ((Offset) -> String?)? = null,
    content: @Composable () -> Unit,
) {
    val gesture = remember { CanvasGestureState() }

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
                    gesture.activePointerSession.isActive(event.getPointerId(event.actionIndex))
                if (rawAction == MotionEvent.ACTION_CANCEL || cancelledUp) {
                    activeStroke.reset()
                    gesture.rulerSession = null
                    gesture.constructionSession = null
                    onConstructionPreview(emptyList())
                    selectionPath.reset()
                    onSelectionRectChanged(null)
                    onEraserCursorChange(null)
                    if (gesture.draggedBone != null) onIntent(StudioIntent.Actor.CancelDrag)
                    gesture.draggedBone = null
                    gesture.consumedTextSelection = false
                    gesture.activePointerSession.clear()
                    gesture.currentToolType = MotionEvent.TOOL_TYPE_UNKNOWN
                    return@pointerInteropFilter true
                }
                val actionTool = event.getToolType(event.actionIndex)
                val stylusContact =
                    actionTool == MotionEvent.TOOL_TYPE_STYLUS || actionTool == MotionEvent.TOOL_TYPE_ERASER
                val stylusTakesOver = studioMode == StudioMode.RASTER_DRAWING &&
                    rawAction == MotionEvent.ACTION_POINTER_DOWN && stylusContact &&
                    gesture.currentToolType != MotionEvent.TOOL_TYPE_STYLUS &&
                    gesture.currentToolType != MotionEvent.TOOL_TYPE_ERASER
                if (stylusTakesOver) {
                    // Discard the provisional finger/palm mark rather than committing it.
                    activeStroke.reset()
                    gesture.rulerSession = null
                    gesture.activePointerSession.begin(event.getPointerId(event.actionIndex), event.eventTime)
                    gesture.currentToolType = actionTool
                }
                if (rawAction == MotionEvent.ACTION_DOWN) {
                    gesture.rigGestureCancelled = false
                    val pointerId = event.getPointerId(event.actionIndex)
                    gesture.activePointerSession.begin(pointerId, event.eventTime)
                    gesture.currentToolType = event.getToolType(event.actionIndex)
                }

                val action = if (stylusTakesOver && rawAction == MotionEvent.ACTION_POINTER_DOWN &&
                    gesture.activePointerSession.isActive(event.getPointerId(event.actionIndex))
                ) {
                    MotionEvent.ACTION_DOWN
                } else {
                    rawAction
                }

                if (studioMode != StudioMode.RASTER_DRAWING &&
                    (event.pointerCount > 1 || action == MotionEvent.ACTION_CANCEL)
                ) {
                    gesture.rigGestureCancelled = true
                    if (gesture.draggedBone != null) onIntent(StudioIntent.Actor.CancelDrag)
                    gesture.draggedBone = null
                    selectionPath.reset()
                    activeStroke.invalidate()
                }
                if (studioMode != StudioMode.RASTER_DRAWING &&
                    gesture.rigGestureCancelled
                ) {
                    return@pointerInteropFilter true
                }

                val activePointerIndex = when (action) {
                    MotionEvent.ACTION_DOWN -> event.actionIndex
                    else -> event.findPointerIndex(gesture.activePointerSession.pointerId)
                }

                if (activePointerIndex < 0) return@pointerInteropFilter true

                // Palm Rejection: If a stylus is active, ignore all finger pointers for drawing
                val toolType = event.getToolType(activePointerIndex)
                if (gesture.currentToolType == MotionEvent.TOOL_TYPE_STYLUS &&
                    toolType == MotionEvent.TOOL_TYPE_FINGER
                ) {
                    return@pointerInteropFilter false
                }

                if (action == MotionEvent.ACTION_POINTER_UP &&
                    gesture.activePointerSession.shouldIgnorePointerUp(event.getPointerId(event.actionIndex))
                ) {
                    return@pointerInteropFilter true
                }

                fun mapToDocument(x: Float, y: Float): Offset? = coordinateTransform.toDocument(Offset(x, y))

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
                        gesture.activePointerSession.end(event.getPointerId(activePointerIndex))
                    }
                    return@pointerInteropFilter true
                }

                val isRiggingTool = activeTool == DrawingTool.RIG_SELECT || activeTool == DrawingTool.BONE

                if (studioMode != StudioMode.RASTER_DRAWING && isRiggingTool) {
                    when (action) {
                        MotionEvent.ACTION_DOWN -> {
                            gesture.draggedBone = onRigJointHit?.invoke(mappedPoint)
                            gesture.rigDownScreen = Offset(screenX, screenY)
                            onIntent(
                                StudioIntent.Rigging.SelectBone(
                                    gesture.draggedBone?.removePrefix(
                                        "origin:",
                                    )?.removePrefix("artwork:")?.removePrefix("attachment:"),
                                ),
                            )
                            gesture.draggedBone?.takeIf { it.startsWith("artwork:") }?.let {
                                onIntent(StudioIntent.Actor.Drag(it, mappedPoint))
                            }
                        }

                        MotionEvent.ACTION_MOVE -> gesture.draggedBone?.let {
                            if ((Offset(screenX, screenY) - gesture.rigDownScreen).getDistance() >
                                4f * displayDensity
                            ) {
                                onIntent(StudioIntent.Actor.Drag(it, mappedPoint))
                            }
                        }

                        MotionEvent.ACTION_UP -> {
                            onIntent(StudioIntent.Actor.CommitDrag)
                            gesture.draggedBone = null
                            gesture.activePointerSession.end(event.getPointerId(activePointerIndex))
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
                        gesture.activePointerSession.end(event.getPointerId(activePointerIndex))
                    }
                    return@pointerInteropFilter true
                }

                if (activeTool == DrawingTool.COLOR_FILL) {
                    when (action) {
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                            val duration = event.eventTime - gesture.activePointerSession.startTime
                            if (duration < 250) {
                                onIntent(StudioIntent.Fill.SetPendingTap(mappedPoint))
                                gesture.activePointerSession.end(event.getPointerId(activePointerIndex))
                                return@pointerInteropFilter true
                            }
                        }
                    }
                    return@pointerInteropFilter true
                }

                if (activeTool == DrawingTool.TEXT) {
                    when (action) {
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                            val duration = event.eventTime - gesture.activePointerSession.startTime
                            if (duration < 200) {
                                val hitText = nodes.values.filterIsInstance<TextNode>().lastOrNull { node ->
                                    textHit(node, mappedPoint, displayDensity, fontRegistry)
                                }
                                if (hitText != null) {
                                    onIntent(StudioIntent.Text.TriggerEditDialog(hitText.id))
                                } else {
                                    onIntent(StudioIntent.Text.TriggerAddTextDialog(mappedX, mappedY))
                                }
                                gesture.activePointerSession.end(event.getPointerId(activePointerIndex))
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
                            gesture.activePointerSession.startX = mappedX
                            gesture.activePointerSession.startY = mappedY
                            activeStroke.invalidate()
                            return@pointerInteropFilter true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            if (selectionTool == SelectionToolType.LASSO) {
                                selectionPath.lineTo(mappedX, mappedY)
                            } else {
                                val startX = gesture.activePointerSession.startX
                                val startY = gesture.activePointerSession.startY
                                onSelectionRectChanged(
                                    Rect(
                                        minOf(startX, mappedX),
                                        minOf(startY, mappedY),
                                        maxOf(startX, mappedX),
                                        maxOf(startY, mappedY),
                                    ),
                                )
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
                                            lassoPath = Path().apply { addPath(selectionPath) },
                                        ),
                                    )
                                }
                                selectionPath.reset()
                                onSelectionRectChanged(null)
                                gesture.activePointerSession.end(event.getPointerId(activePointerIndex))
                                return@pointerInteropFilter true
                            }
                            if (gesture.consumedTextSelection) {
                                gesture.consumedTextSelection = false
                                gesture.activePointerSession.end(event.getPointerId(activePointerIndex))
                                return@pointerInteropFilter true
                            }
                            val distance = (
                                mappedPoint -
                                    Offset(gesture.activePointerSession.startX, gesture.activePointerSession.startY)
                                ).getDistance()
                            if (distance < 2f && selectionPath.getBounds().width < 3f &&
                                selectionPath.getBounds().height < 3f
                            ) {
                                onSelectionAreaComplete(
                                    null,
                                    Rect(
                                        mappedX - .5f,
                                        mappedY - .5f,
                                        mappedX + .5f,
                                        mappedY + .5f,
                                    ),
                                    displayDensity,
                                    fontRegistry,
                                )
                            } else if (selectionTool == SelectionToolType.LASSO) {
                                selectionPath.close()
                                val completedPath = Path().apply { addPath(selectionPath) }
                                onSelectionAreaComplete(completedPath, null, displayDensity, fontRegistry)
                            } else {
                                val startX = gesture.activePointerSession.startX
                                val startY = gesture.activePointerSession.startY
                                val rect = Rect(
                                    minOf(startX, mappedX),
                                    minOf(startY, mappedY),
                                    maxOf(startX, mappedX),
                                    maxOf(startY, mappedY),
                                )
                                onSelectionAreaComplete(null, rect, displayDensity, fontRegistry)
                            }
                            selectionPath.reset()
                            onSelectionRectChanged(null)
                            gesture.activePointerSession.end(event.getPointerId(activePointerIndex))
                            return@pointerInteropFilter true
                        }
                    }
                }

                val canStartConstruction = ConstructionGesturePolicy.canHandle(
                    activeTool,
                    constructionShape,
                    constructionPreviewPending,
                )
                if (constructionShape != null && constructionPreviewPending && gesture.constructionSession == null) {
                    // A pending construction preview owns the canvas until Check or X;
                    // never fall through to freehand Brush input.
                    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        gesture.activePointerSession.end(event.getPointerId(activePointerIndex))
                    }
                    return@pointerInteropFilter true
                }
                if (canStartConstruction || gesture.constructionSession != null) {
                    when (action) {
                        MotionEvent.ACTION_DOWN -> {
                            val session = ConstructionShapeSession(requireNotNull(constructionShape))
                            session.begin(
                                TouchPoint(
                                    mappedX,
                                    mappedY,
                                    event.getPressure(activePointerIndex),
                                    timestamp = event.eventTime,
                                ),
                            )
                            gesture.constructionSession = session
                            return@pointerInteropFilter true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            gesture.constructionSession?.let { session ->
                                val sample =
                                    TouchPoint(
                                        mappedX,
                                        mappedY,
                                        event.getPressure(activePointerIndex),
                                        timestamp = event.eventTime,
                                    )
                                session.update(sample)
                                session.preview()?.let(onConstructionPreview)
                            }
                            return@pointerInteropFilter true
                        }

                        MotionEvent.ACTION_UP -> {
                            gesture.constructionSession?.complete()?.let(onConstructionPreview)
                            gesture.constructionSession = null
                            gesture.activePointerSession.end(event.getPointerId(activePointerIndex))
                            return@pointerInteropFilter true
                        }

                        MotionEvent.ACTION_CANCEL -> {
                            gesture.constructionSession = null
                            onConstructionPreview(emptyList())
                            gesture.activePointerSession.clear()
                            return@pointerInteropFilter true
                        }
                    }
                }

                if (activeTool == DrawingTool.ERASER) {
                    when (action) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> onEraserCursorChange(
                            Offset(screenX, screenY),
                        )

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL ->
                            onEraserCursorChange(null)
                    }
                }

                // If lasso or text is active, don't fall through to general stroke handling
                if (activeTool == DrawingTool.LASSO || activeTool == DrawingTool.LASSO_CUT ||
                    activeTool == DrawingTool.TEXT
                ) {
                    return@pointerInteropFilter true
                }

                if (action == MotionEvent.ACTION_DOWN) {
                    val currentTime = System.currentTimeMillis()
                    val tapPoint = Offset(screenX, screenY)

                    // Double tap detection
                    if (currentTime - gesture.lastTapTime < 300L &&
                        (tapPoint - gesture.lastTapPoint).getDistance() < 50f
                    ) {
                        val hitText = nodes.values.filterIsInstance<TextNode>().lastOrNull { node ->
                            textHit(node, mappedPoint, displayDensity, fontRegistry)
                        }
                        if (hitText != null) {
                            onIntent(StudioIntent.Text.TriggerEditDialog(hitText.id))
                            gesture.lastTapTime = 0L // Reset to prevent triple tap as double double tap
                            return@pointerInteropFilter true
                        }
                    }

                    gesture.lastTapTime = currentTime
                    gesture.lastTapPoint = tapPoint
                }

                gesture.handleFreehandStroke(
                    event,
                    CanvasPointer(action, activePointerIndex, Offset(screenX, screenY), mappedPoint, isWithinDocument),
                    StrokeTools(
                        activeTool,
                        pressureSensitive,
                        displayDensity,
                        rulerGuide,
                        manipulationSource,
                        manipulationWidth,
                        manipulationStrength,
                    ),
                    activeStroke,
                    coordinateTransform,
                ) { points ->
                    onStrokeSettled(points)
                    onStrokeComplete(points)
                }
            },
    ) {
        content()
    }
}
