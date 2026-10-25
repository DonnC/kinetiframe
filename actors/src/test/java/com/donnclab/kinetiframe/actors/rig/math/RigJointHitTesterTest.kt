package com.donnclab.kinetiframe.actors.rig.math

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class RigJointHitTesterTest {
    @Test
    fun `chooses the closest joint inside the hit radius`() {
        val result = RigJointHitTester.nearestWithin(
            point = Offset(12f, 10f),
            joints = listOf(RigJointTarget("far", Offset(40f, 10f)), RigJointTarget("near", Offset(10f, 10f))),
            radius = 12f,
        )
        assertEquals("near", result)
    }

    @Test
    fun `returns null outside the hit radius`() {
        assertEquals(
            null,
            RigJointHitTester.nearestWithin(Offset.Zero, listOf(RigJointTarget("joint", Offset(20f, 0f))), 10f),
        )
    }
}
