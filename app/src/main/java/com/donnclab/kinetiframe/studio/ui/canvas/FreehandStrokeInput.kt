package com.donnclab.kinetiframe.studio.ui.canvas

import android.graphics.Bitmap
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.project.model.DrawingTool
import com.donnclab.kinetiframe.project.model.TouchPoint
import com.donnclab.kinetiframe.studio.RulerGuide
import com.donnclab.kinetiframe.studio.RulerMode
import com.donnclab.kinetiframe.studio.drawing.RulerStrokeConstraintSession

/** The active pointer of one [MotionEvent], in screen and document coordinates. */
internal data class CanvasPointer(
    val action: Int,
    val index: Int,
    val screen: Offset,
    val document: Offset,
    val withinDocument: Boolean,
)

/** How a freehand stroke is drawn: the tool, pressure response, and any guide or pixel source. */
internal data class StrokeTools(
    val activeTool: DrawingTool,
    val pressureSensitive: Boolean,
    val displayDensity: Float,
    val rulerGuide: RulerGuide?,
    /** Raw pixels of the selected layer. Manipulation must never sample the composited frame. */
    val manipulationSource: Bitmap?,
    val manipulationWidth: Float,
    val manipulationStrength: Float,
)

/**
 * Feeds one pointer event into the freehand [stroke] and commits it on release. Returns whether
 * the event was handled.
 */
internal fun CanvasGestureState.handleFreehandStroke(
    event: MotionEvent,
    pointer: CanvasPointer,
    tools: StrokeTools,
    stroke: ActiveStroke,
    transform: DocumentCoordinateTransform,
    onComplete: (List<TouchPoint>) -> Unit,
): Boolean {
    val mirror = tools.rulerGuide?.mode == RulerMode.MIRROR
    val index = pointer.index
    val (x, y) = pointer.document
    return when (pointer.action) {
        MotionEvent.ACTION_DOWN -> {
            stroke.reset()
            stroke.configureInput(tools.displayDensity, tools.pressureSensitive)
            stroke.isStylusContact = currentToolType == MotionEvent.TOOL_TYPE_STYLUS ||
                currentToolType == MotionEvent.TOOL_TYPE_ERASER
            val manipulates = tools.activeTool == DrawingTool.BLUR || tools.activeTool == DrawingTool.SMUDGE
            if (manipulates && tools.manipulationSource != null) {
                stroke.beginManipulation(
                    source = tools.manipulationSource,
                    tool = tools.activeTool,
                    baseWidth = tools.manipulationWidth,
                    strength = tools.manipulationStrength,
                )
            }
            rulerSession = if (tools.activeTool == DrawingTool.BRUSH && tools.rulerGuide != null) {
                RulerStrokeConstraintSession(tools.rulerGuide.toConstraintSpec())
            } else {
                null
            }
            appendGuided(stroke, event.documentSample(index, x, y), event.screenSample(pointer), mirror)
            stroke.invalidate()
            true
        }

        MotionEvent.ACTION_MOVE -> {
            val previousPointCount = stroke.points.size
            for (i in 0 until event.historySize) {
                val screenX = event.getHistoricalX(index, i)
                val screenY = event.getHistoricalY(index, i)
                val historical = transform.toDocument(Offset(screenX, screenY))
                if (historical != null && transform.contains(historical)) {
                    val pressure = event.getHistoricalPressure(index, i)
                    val time = event.getHistoricalEventTime(i)
                    val sample = TouchPoint(
                        historical.x,
                        historical.y,
                        pressure,
                        event.getHistoricalAxisValue(MotionEvent.AXIS_TILT, index, i),
                        event.getHistoricalAxisValue(MotionEvent.AXIS_ORIENTATION, index, i),
                        velocity = 0f,
                        timestamp = time,
                    )
                    val screenSample = TouchPoint(screenX, screenY, pressure, velocity = 0f, timestamp = time)
                    appendGuided(stroke, sample, screenSample, mirror)
                }
            }
            appendGuided(stroke, event.documentSample(index, x, y), event.screenSample(pointer), mirror)

            // Raw prediction bypassed conditioning and made a held nib visibly dance.
            // Render the latest conditioned contact; prediction is not committed ink.
            stroke.predictedTouchPoint = null
            stroke.predictedMirroredTouchPoint = null

            if (stroke.points.size != previousPointCount) stroke.invalidate()
            true
        }

        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
            val last = stroke.points.lastOrNull()
            val manipulationLift = (tools.activeTool == DrawingTool.SMUDGE || tools.activeTool == DrawingTool.BLUR) &&
                last?.timestamp != event.eventTime
            if (pointer.withinDocument && (last == null || last.x != x || last.y != y || manipulationLift)) {
                stroke.flushSample(
                    event.documentSample(index, x, y),
                    constraint = rulerSession?.let { session -> session::constrain },
                    screenSample = TouchPoint(pointer.screen.x, pointer.screen.y, timestamp = event.eventTime),
                ).also { constrained ->
                    if (mirror) stroke.appendMirroredSample(requireNotNull(rulerSession).reflect(constrained))
                }
            }
            // Commit exactly the stabilized points the live preview showed. Simplifying
            // the stroke on release would visibly change its shape and pressure at the
            // moment it becomes a bitmap.
            stroke.predictedTouchPoint = null
            onComplete(ArrayList(stroke.points))
            stroke.reset()
            rulerSession = null
            activePointerSession.end(event.getPointerId(index))
            currentToolType = MotionEvent.TOOL_TYPE_UNKNOWN
            true
        }

        MotionEvent.ACTION_CANCEL -> {
            stroke.reset()
            rulerSession = null
            activePointerSession.clear()
            currentToolType = MotionEvent.TOOL_TYPE_UNKNOWN
            true
        }

        MotionEvent.ACTION_POINTER_DOWN -> true

        else -> false
    }
}

/** Appends a sample through the active ruler, and its reflection when the ruler mirrors. */
private fun CanvasGestureState.appendGuided(
    stroke: ActiveStroke,
    sample: TouchPoint,
    screenSample: TouchPoint,
    mirror: Boolean,
) {
    stroke.appendSample(
        sample = sample,
        screenSample = screenSample,
        constraint = rulerSession?.let { session -> session::constrain },
    )?.also { constrained ->
        if (mirror) stroke.appendMirroredSample(requireNotNull(rulerSession).reflect(constrained))
    }
}

private fun MotionEvent.documentSample(index: Int, x: Float, y: Float) = TouchPoint(
    x,
    y,
    getPressure(index),
    getAxisValue(MotionEvent.AXIS_TILT, index),
    getAxisValue(MotionEvent.AXIS_ORIENTATION, index),
    velocity = 0f,
    timestamp = eventTime,
)

private fun MotionEvent.screenSample(pointer: CanvasPointer) =
    TouchPoint(pointer.screen.x, pointer.screen.y, getPressure(pointer.index), velocity = 0f, timestamp = eventTime)
