package com.donnclab.kinetiframe.studio.domain

import android.graphics.Matrix
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class FrameStageRectTest {
    private var state = StudioState().let {
        it.copy(canvas = it.canvas.copy(viewportWidth = 2000, viewportHeight = 1000))
    }
    private val views = ViewManager { change -> state = change(state) }

    private fun screen(x: Float, y: Float): FloatArray =
        floatArrayOf(x, y).also { Matrix().apply { setValues(state.canvas.matrixValues) }.mapPoints(it) }

    @Test fun `an actor's bounds fill the canvas above the bottom panels, centred`() {
        // A tall actor on the stage; a quarter of the canvas is covered at the bottom.
        views.handle(StudioIntent.View.FrameStageRect(500f, 200f, 700f, 600f, fill = 0.8f, bottomInset = 0.25f))
        val top = screen(600f, 200f)
        val bottom = screen(600f, 600f)
        // 400 stage units become 80% of the 750 clear pixels.
        assertEquals(600f, bottom[1] - top[1], 0.5f)
        // Centred horizontally, and in the clear 750 pixels vertically.
        assertEquals(1000f, top[0], 0.5f)
        assertEquals(375f, (top[1] + bottom[1]) / 2f, 0.5f)
    }

    @Test fun `pinching afterwards continues from the framed view`() {
        views.handle(StudioIntent.View.FrameStageRect(500f, 200f, 700f, 600f, fill = 0.8f))
        val zoom = state.canvas.zoom
        views.handle(
            StudioIntent.View.UpdateTransform(
                com.donnclab.kinetiframe.core.geometry.Point2.Zero,
                1f,
                0f,
                com.donnclab.kinetiframe.core.geometry.Point2.Zero,
            ),
        )
        assertEquals(zoom, state.canvas.zoom, 0.0001f)
    }
}
