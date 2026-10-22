package com.donnclab.kinetiframe.studio.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import com.donnclab.kinetiframe.studio.SelectionInteractionMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SelectionHandleHitTestTest {
    private val pivot = Offset(50f, 50f)
    private fun hit(
        point: Offset,
        mode: SelectionInteractionMode = SelectionInteractionMode.TRANSFORM,
        scale: Float = 1f,
        handles: List<Pair<Int, Offset>> = emptyList(),
    ) = SelectionHandleHitTest.resolve(point, handles, pivot, pivot, mode, scale)

    @Test fun `pivot only captures direct contact on its visible ring`() {
        assertEquals(4, hit(pivot))
        assertEquals(4, hit(pivot + Offset(8f, 0f)))
        assertEquals(-1, hit(pivot + Offset(8.1f, 0f)))
        assertEquals(-1, hit(pivot + Offset(20f, 0f)))
    }

    @Test fun `pivot hit area stays screen sized at different zooms and densities`() {
        for (scale in listOf(.5f, 1f, 2f, 4f)) {
            assertEquals(4, hit(pivot + Offset(7f / scale, 0f), scale = scale))
            assertEquals(-1, hit(pivot + Offset(9f / scale, 0f), scale = scale))
        }
    }

    @Test fun `near pivot contact can still reach resize handle`() {
        assertEquals(0, hit(Offset(62f, 50f), handles = listOf(0 to Offset(70f, 50f))))
    }

    @Test fun `small selection move handle overrides pivot and overlapping resize targets`() {
        val handles = listOf(0 to Offset(48f, 48f), 1 to Offset(52f, 48f))
        assertEquals(10, hit(pivot, SelectionInteractionMode.MOVE, handles = handles))
        assertEquals(10, hit(pivot + Offset(20f, 0f), SelectionInteractionMode.MOVE, handles = handles))
        assertEquals(-1, hit(pivot + Offset(25f, 0f), SelectionInteractionMode.MOVE, handles = handles))
    }

    @Test fun `move translates tiny rotated selection without resizing or moving local pivot`() {
        val bounds = Rect(0f, 0f, 3f, 4f)
        val start = Matrix().apply {
            rotateZ(35f)
            scale(1.5f, .75f)
        }
        val touch = start.map(bounds.center)
        val result = SelectionTransformMath.compute(
            start,
            bounds,
            SelectionHandleHitTest.MOVE,
            touch,
            touch + Offset(42f, -18f),
        )
        for (index in listOf(0, 1, 4, 5)) assertEquals(start.values[index], result.values[index], .0001f)
        assertEquals(42f, (result.map(bounds.center) - start.map(bounds.center)).x, .001f)
        assertEquals(-18f, (result.map(bounds.center) - start.map(bounds.center)).y, .001f)
    }

    @Test fun `move handle uses box center rather than relocated rotation pivot`() {
        assertEquals(
            10,
            SelectionHandleHitTest.resolve(
                Offset.Zero,
                emptyList(),
                pivot,
                Offset.Zero,
                SelectionInteractionMode.MOVE,
                1f,
            ),
        )
        assertEquals(
            -1,
            SelectionHandleHitTest.resolve(
                pivot,
                emptyList(),
                pivot,
                Offset.Zero,
                SelectionInteractionMode.MOVE,
                1f,
            ),
        )
    }
}
