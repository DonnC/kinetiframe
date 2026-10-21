// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.core.actor.rigging.Transform
import com.donnclab.kinetiframe.core.geometry.Point2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class RigTransformMathTest {
    @Test fun `translation rotation and reflected per axis scale preserve the original basis`() {
        val transform = Transform(Point2(10f, 20f), 90f, Point2(-2f, 3f))
        val matrix = RigTransformMath.local(transform)
        assertEquals(4f, matrix.x(1f, 2f), .001f)
        assertEquals(18f, matrix.y(1f, 2f), .001f)
        assertSame(transform.translation, matrix.origin)
    }

    @Test fun `parent child composition applies parent scale and rotation to child position`() {
        val parent = RigTransformMath.local(Transform(Point2(10f, 20f), 90f, Point2(2f, 3f)))
        val child = RigTransformMath.local(Transform(5f, 0f))
        val world = parent.times(child)
        assertEquals(10f, world.x(0f, 0f), .001f)
        assertEquals(30f, world.y(0f, 0f), .001f)
        assertEquals(4f, world.x(2f, 2f), .001f)
        assertEquals(34f, world.y(2f, 2f), .001f)
    }
}
