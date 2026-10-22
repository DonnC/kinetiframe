package com.donnclab.kinetiframe.studio.drawing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class Affine2DTest {
    @Test fun stageCompositionPreservesSkewReflectionAndPosition() {
        val old = Affine2D(a = -2f, c = .3f, d = 3f, tx = 7f, ty = 8f)
        val edit = Affine2D(b = .5f, tx = 20f, ty = -4f)
        val result = edit * old
        assertEquals(edit.mapX(old.mapX(11f, 13f), old.mapY(11f, 13f)), result.mapX(11f, 13f), .0001f)
        assertEquals(edit.mapY(old.mapX(11f, 13f), old.mapY(11f, 13f)), result.mapY(11f, 13f), .0001f)
        assertEquals(-2f, result.a)
    }

    @Test fun identityDoesNotLoseAnyCoefficient() {
        val transform = Affine2D(.2f, -.7f, .5f, 2f, 11f, -12f)
        assertEquals(transform, Affine2D() * transform)
        assertEquals(transform, transform * Affine2D())
    }

    @Test fun nonFiniteDataRejectedBeforeRendering() {
        assertThrows(IllegalArgumentException::class.java) { Affine2D(c = Float.NaN) }
    }
}
