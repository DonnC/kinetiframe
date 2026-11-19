package com.donnclab.kinetiframe.actors.rig.math

import com.donnclab.kinetiframe.core.geometry.Point2
import org.junit.Assert.assertEquals
import org.junit.Test

class RigJointHitTesterTest {
    @Test
    fun `chooses the closest joint inside the hit radius`() {
        val result = RigJointHitTester.nearestWithin(
            point = Point2(12f, 10f),
            joints = listOf(RigJointTarget("far", Point2(40f, 10f)), RigJointTarget("near", Point2(10f, 10f))),
            radius = 12f,
        )
        assertEquals("near", result)
    }

    @Test
    fun `returns null outside the hit radius`() {
        assertEquals(
            null,
            RigJointHitTester.nearestWithin(Point2.Zero, listOf(RigJointTarget("joint", Point2(20f, 0f))), 10f),
        )
    }
}
