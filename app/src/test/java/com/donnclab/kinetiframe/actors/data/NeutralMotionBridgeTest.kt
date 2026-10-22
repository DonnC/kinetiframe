package com.donnclab.kinetiframe.actors.data

import com.donnclab.kinetiframe.core.actor.motion.MotionValidationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NeutralMotionBridgeTest {
    @Test fun `legacy validation translates typed failures without changing diagnostics`() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            RotationCurve("arm", points = emptyList()).validate(1)
        }
        assertEquals("Curve needs points", failure.message)
        assertTrue(failure.cause is MotionValidationException)
        val root = assertThrows(IllegalArgumentException::class.java) {
            RootMotionCurve("body", emptyList()).validate(1)
        }
        assertEquals("Root motion point budget exceeded", root.message)
    }

    @Test fun `compatibility import is the same public model not a copy`() {
        val curve = RotationCurve("arm", points = listOf(RotationCurvePoint(0, 10f)))
        val publicModel: com.donnclab.kinetiframe.core.actor.motion.RotationCurve = curve
        assertSame(curve, publicModel)
        assertSame(curve, curve.validate(1))
    }
}
