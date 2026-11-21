package com.donnclab.kinetiframe.presentation.studio.timeline

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ClipDragMathTest {
    @Test fun `small events accumulate into an exact frame`() {
        val drag = ClipDragMath(40f)
        repeat(39) { drag.add(1f) }
        assertEquals(1, drag.add(1f))
    }

    @Test fun `reversing returns to the original frame without rounding drift`() {
        val drag = ClipDragMath(40f)
        repeat(100) { drag.add(.7f) }
        repeat(99) { drag.add(-.7f) }
        assertEquals(0, drag.add(-.7f))
    }

    @Test fun `invalid input does not poison a gesture`() {
        val drag = ClipDragMath(40f)
        drag.add(Float.NaN)
        assertEquals(-2, drag.add(-80f))
        assertEquals(0, ClipDragMath(0f).add(80f))
    }
}
