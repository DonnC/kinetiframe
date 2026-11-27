// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.domain

import android.graphics.Matrix
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class FillViewTest {
    private var state = StudioState().let {
        // A view squarer than the 16:9 stage, so fitting leaves bands above and below.
        it.copy(canvas = it.canvas.copy(viewportWidth = 1600, viewportHeight = 1200))
    }
    private val views = ViewManager { change -> state = change(state) }

    private fun screen(x: Float, y: Float): FloatArray =
        floatArrayOf(x, y).also { Matrix().apply { setValues(state.canvas.matrixValues) }.mapPoints(it) }

    @Test fun `fill covers the whole view, then fit shows the whole canvas again`() {
        val stage = state.project.stage
        views.handle(StudioIntent.View.ToggleFillView)
        val topLeft = screen(0f, 0f)
        val bottomRight = screen(stage.width.toFloat(), stage.height.toFloat())
        assertEquals(0f, topLeft[1], 0.5f)
        assertEquals(1200f, bottomRight[1], 0.5f)
        assertTrue(topLeft[0] < 0f && bottomRight[0] > 1600f)

        views.handle(StudioIntent.View.ToggleFillView)
        assertFalse(state.isCanvasTransformed)
    }

    @Test fun `a moved view fits rather than fills`() {
        views.handle(StudioIntent.View.ToggleFillView)
        views.handle(StudioIntent.View.ToggleFillView)
        views.handle(
            StudioIntent.View.UpdateTransform(
                com.donnclab.kinetiframe.core.geometry.Point2(40f, 0f),
                1f,
                0f,
                com.donnclab.kinetiframe.core.geometry.Point2.Zero,
            ),
        )
        views.handle(StudioIntent.View.ToggleFillView)
        assertFalse(state.isCanvasTransformed)
    }
}
