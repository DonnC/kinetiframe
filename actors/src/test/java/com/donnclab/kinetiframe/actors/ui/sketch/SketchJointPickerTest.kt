package com.donnclab.kinetiframe.actors.ui.sketch

import androidx.compose.ui.geometry.Offset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SketchJointPickerTest {
    @Test fun `tiny attachment has only its position handle while a limb retains both ends`() {
        assertTrue(SketchJointPicker.handles("eyes", Offset.Zero, Offset(1f, 0f), true).single().start)
        assertEquals(
            listOf(true, false),
            SketchJointPicker.handles("arm", Offset.Zero, Offset(40f, 0f), false).map {
                it.start
            },
        )
    }

    @Test fun `a closer eye pin wins over a selected torso inside the touch radius`() {
        val eye = SketchJointHandle("eyes", true, Offset.Zero)
        val torso = SketchJointHandle("torso", false, Offset(20f, 0f))
        assertEquals(eye, SketchJointPicker.pick(listOf(torso, eye), Offset.Zero, 24f, 3f, "torso"))
    }

    @Test fun `named selection resolves coincident profile pins and empty space is ignored`() {
        val left = SketchJointHandle("left_hand", true, Offset.Zero)
        val right = SketchJointHandle("right_hand", true, Offset.Zero)
        assertEquals(right, SketchJointPicker.pick(listOf(left, right), Offset.Zero, 24f, 3f, "right_hand"))
        assertNull(SketchJointPicker.pick(listOf(left, right), Offset(50f, 50f), 24f, 3f, "right_hand"))
    }
}
