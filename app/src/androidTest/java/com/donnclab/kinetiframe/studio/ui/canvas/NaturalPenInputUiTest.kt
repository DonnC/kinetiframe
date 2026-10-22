package com.donnclab.kinetiframe.studio.ui.canvas

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import com.donnclab.kinetiframe.project.model.DrawingTool
import com.donnclab.kinetiframe.project.model.StageDimensions
import com.donnclab.kinetiframe.project.model.TouchPoint
import com.donnclab.kinetiframe.studio.SelectionToolType
import com.donnclab.kinetiframe.studio.StudioMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Real MotionEvents through Compose's native interop boundary; no stylus hardware assumption. */
class NaturalPenInputUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var view: View
    private lateinit var stroke: ActiveStroke
    private val completed = mutableListOf<List<TouchPoint>>()

    private fun setup() {
        stroke = ActiveStroke()
        compose.setContent {
            view = LocalView.current
            CanvasInputLayer(
                stage = StageDimensions(10000, 10000),
                canvasMatrixValues = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f),
                sourceFrame = null, studioMode = StudioMode.RASTER_DRAWING,
                activeTool = DrawingTool.BRUSH, rulerGuide = null, selectionActive = false,
                nodes = emptyMap(), displayDensity = 1f, fontRegistry = null,
                selectionTool = SelectionToolType.LASSO, activeStroke = stroke, selectionPath = Path(),
                onSelectionRectChanged = {}, onTextNodeSelect = {}, onSelectionAreaComplete = { _, _, _, _ -> },
                onEraserCursorChange = {}, onIntent = {}, onStrokeSettled = {},
                onStrokeComplete = { completed += it.toList() },
            ) {}
        }
        compose.waitForIdle()
    }

    private fun event(down: Long, time: Long, action: Int, tools: List<Int>, xs: List<Float>) {
        compose.runOnIdle {
            val properties = Array(tools.size) { i ->
                MotionEvent.PointerProperties().apply {
                    id = i
                    toolType = tools[i]
                }
            }
            val coords = Array(tools.size) { i ->
                MotionEvent.PointerCoords().apply {
                    x = xs[i]
                    y = 80f
                    pressure = 0.5f
                    size = 0.05f
                }
            }
            val e = MotionEvent.obtain(
                down, time, action, tools.size, properties, coords,
                0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0,
            )
            try {
                view.dispatchTouchEvent(e)
            } finally {
                e.recycle()
            }
        }
    }

    @Test fun finger_capacitive_pen_unknown_and_native_stylus_can_all_draw() {
        setup()
        for (tool in listOf(
            MotionEvent.TOOL_TYPE_FINGER,
            MotionEvent.TOOL_TYPE_UNKNOWN,
            MotionEvent.TOOL_TYPE_STYLUS,
            MotionEvent.TOOL_TYPE_ERASER,
        )) {
            val start = SystemClock.uptimeMillis()
            event(start, start, MotionEvent.ACTION_DOWN, listOf(tool), listOf(40f))
            event(start, start + 16, MotionEvent.ACTION_MOVE, listOf(tool), listOf(90f))
            event(start, start + 32, MotionEvent.ACTION_UP, listOf(tool), listOf(100f))
        }
        assertEquals(4, completed.size)
        assertTrue(completed.all { it.size >= 2 && it.last().x > it.first().x })
    }

    @Test fun palm_pointer_lifting_does_not_finish_the_stylus_stroke() {
        setup()
        val start = SystemClock.uptimeMillis()
        val pen = MotionEvent.TOOL_TYPE_STYLUS
        val finger = MotionEvent.TOOL_TYPE_FINGER
        event(start, start, MotionEvent.ACTION_DOWN, listOf(pen), listOf(40f))
        event(start, start + 8, MotionEvent.ACTION_POINTER_DOWN or (1 shl 8), listOf(pen, finger), listOf(50f, 200f))
        event(start, start + 16, MotionEvent.ACTION_MOVE, listOf(pen, finger), listOf(80f, 240f))
        event(start, start + 24, MotionEvent.ACTION_POINTER_UP or (1 shl 8), listOf(pen, finger), listOf(80f, 240f))
        assertTrue(completed.isEmpty())
        event(start, start + 32, MotionEvent.ACTION_UP, listOf(pen), listOf(100f))
        assertEquals(1, completed.size)
        assertTrue(completed.single().all { it.x < 110f })
    }

    @Test fun native_stylus_takes_over_a_provisional_finger_contact() {
        setup()
        val start = SystemClock.uptimeMillis()
        val finger = MotionEvent.TOOL_TYPE_FINGER
        val pen = MotionEvent.TOOL_TYPE_STYLUS
        event(start, start, MotionEvent.ACTION_DOWN, listOf(finger), listOf(200f))
        event(start, start + 8, MotionEvent.ACTION_POINTER_DOWN or (1 shl 8), listOf(finger, pen), listOf(200f, 40f))
        event(start, start + 16, MotionEvent.ACTION_MOVE, listOf(finger, pen), listOf(220f, 90f))
        event(start, start + 24, MotionEvent.ACTION_POINTER_UP or (1 shl 8), listOf(finger, pen), listOf(220f, 100f))
        assertEquals(1, completed.size)
        assertTrue(completed.single().all { it.x < 110f })
        event(start, start + 32, MotionEvent.ACTION_UP, listOf(finger), listOf(220f))
        assertEquals(1, completed.size)
    }

    @Test fun cancelled_contact_is_not_committed() {
        setup()
        val start = SystemClock.uptimeMillis()
        event(start, start, MotionEvent.ACTION_DOWN, listOf(MotionEvent.TOOL_TYPE_STYLUS), listOf(40f))
        event(start, start + 16, MotionEvent.ACTION_CANCEL, listOf(MotionEvent.TOOL_TYPE_STYLUS), listOf(60f))
        assertTrue(completed.isEmpty())
        assertTrue(stroke.points.isEmpty())
    }
}
