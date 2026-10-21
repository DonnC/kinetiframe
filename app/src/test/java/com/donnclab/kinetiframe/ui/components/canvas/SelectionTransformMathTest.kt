package com.donnclab.kinetiframe.ui.components.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.sin

class SelectionTransformMathTest {
    private val bounds = Rect(0f, 0f, 100f, 100f)

    @Test fun allCornersResizeAroundOppositeCorner() {
        val corners = listOf(bounds.topLeft, bounds.topRight, bounds.bottomRight, bounds.bottomLeft)
        corners.forEachIndexed { handle, touch ->
            val pivot = corners[(handle + 2) % 4]
            val delta = Offset(if (touch.x == 0f) -30f else 30f, if (touch.y == 0f) -10f else 10f)
            val result = SelectionTransformMath.compute(
                Matrix(),
                bounds,
                handle,
                touch,
                touch + delta,
                lockAspectRatio = false,
            )
            assertEquals((touch + delta).x, result.map(touch).x, .001f)
            assertEquals((touch + delta).y, result.map(touch).y, .001f)
            assertEquals(pivot.x, result.map(pivot).x, .001f)
            assertEquals(pivot.y, result.map(pivot).y, .001f)
        }
    }

    @Test fun offsetHandleContactDoesNotJump() {
        val touch = Offset(-8f, -6f)
        val result = SelectionTransformMath.compute(
            Matrix(),
            bounds,
            0,
            touch,
            touch + Offset(-10f, -15f),
            lockAspectRatio = false,
        )
        assertEquals(-10f, (result.map(touch) - touch).x, .001f)
        assertEquals(-15f, (result.map(touch) - touch).y, .001f)
    }

    @Test fun customRotationAnchorStaysFixed() {
        val pivot = Offset(-20f, 80f)
        val touch = Offset(50f, -48f)
        val relative = touch - pivot
        val result = SelectionTransformMath.compute(
            Matrix(),
            bounds,
            5,
            touch,
            pivot + Offset(-relative.y, relative.x),
            customPivotDoc = pivot,
        )
        assertEquals(pivot.x, result.map(pivot).x, .001f)
        assertEquals(pivot.y, result.map(pivot).y, .001f)
    }

    @Test fun skewEachSideKeepsOppositeSideStationary() {
        listOf(6, 7, 8, 9).forEach { handle ->
            val touch = when (handle) {
                6 -> Offset(50f, 0f)
                7 -> Offset(100f, 50f)
                8 -> Offset(50f, 100f)
                else -> Offset(0f, 50f)
            }
            val pivot = bounds.center * 2f - touch
            val delta = if (handle == 6 || handle == 8) Offset(25f, 0f) else Offset(0f, 25f)
            val result = SelectionTransformMath.compute(Matrix(), bounds, handle, touch, touch + delta, skewMode = true)
            assertEquals((touch + delta).x, result.map(touch).x, .001f)
            assertEquals((touch + delta).y, result.map(touch).y, .001f)
            assertEquals(pivot.x, result.map(pivot).x, .001f)
            assertEquals(pivot.y, result.map(pivot).y, .001f)
        }
    }

    @Test fun scaleCrossingDoesNotProduceSingularMatrix() {
        val result = SelectionTransformMath.compute(
            Matrix(),
            bounds,
            0,
            Offset.Zero,
            Offset(100f, 100f),
            lockAspectRatio = false,
        )
        assertEquals(.01f, result.values[0], .0001f)
        assertEquals(.01f, result.values[5], .0001f)
    }

    @Test
    fun pinpointAccuracyForCornerScaling() {
        val startMatrix = Matrix()
        val startTouch = Offset(0f, 0f) // Top-Left
        val currentTouch = Offset(-50f, -50f) // Stretch TL corner out

        // Handle 0 is Top-Left, lockAspectRatio = true by default
        val result = SelectionTransformMath.compute(
            startMatrix = startMatrix,
            bounds = bounds,
            handle = 0,
            startTouchDoc = startTouch,
            currentTouchDoc = currentTouch,
            lockAspectRatio = true,
        )

        // With uniform scaling, it should take the max delta.
        // dx = -50, dy = -50. Scale is (100+50)/100 = 1.5
        val transformedTL = result.map(Offset(0f, 0f))
        assertEquals(-50f, transformedTL.x, 0.001f)
        assertEquals(-50f, transformedTL.y, 0.001f)
    }

    @Test
    fun nonUniformCornerScalingWhenUnlocked() {
        val startMatrix = Matrix()
        val startTouch = Offset(0f, 0f)
        val currentTouch = Offset(-50f, -20f)

        val result = SelectionTransformMath.compute(
            startMatrix = startMatrix,
            bounds = bounds,
            handle = 0,
            startTouchDoc = startTouch,
            currentTouchDoc = currentTouch,
            lockAspectRatio = false,
        )

        val transformedTL = result.map(Offset(0f, 0f))
        assertEquals(-50f, transformedTL.x, 0.001f)
        assertEquals(-20f, transformedTL.y, 0.001f)
    }

    @Test
    fun pinpointAccuracyForRotation() {
        val startMatrix = Matrix()
        val startTouch = Offset(50f, -48f) // Rotation handle local
        val currentTouch = Offset(148f, 50f) // 90 degrees right, preserving the 98px radius.

        // Handle 5 is Rotation
        val result = SelectionTransformMath.compute(startMatrix, bounds, 5, startTouch, currentTouch)

        // The rotation handle point after transform should be EXACTLY at currentTouch
        val transformedHandle = result.map(startTouch)
        assertEquals(currentTouch.x, transformedHandle.x, 0.001f)
        assertEquals(currentTouch.y, transformedHandle.y, 0.001f)

        // Pivot (Center, 50,50) should NOT have moved
        val transformedCenter = result.map(Offset(50f, 50f))
        assertEquals(50f, transformedCenter.x, 0.001f)
        assertEquals(50f, transformedCenter.y, 0.001f)
    }

    @Test
    fun inwardPointerDuringRotationPreservesRadius() {
        val start = Offset(50f, -48f)
        val pivot = Offset(50f, 50f)
        val result = SelectionTransformMath.compute(Matrix(), bounds, 5, start, Offset(100f, 50f))
        val handle = result.map(start)
        assertEquals(98f, (handle - pivot).getDistance(), 0.001f)
        assertEquals(148f, handle.x, 0.001f)
        assertEquals(50f, handle.y, 0.001f)
        assertEquals(pivot.x, result.map(pivot).x, 0.001f)
        assertEquals(pivot.y, result.map(pivot).y, 0.001f)
    }

    @Test
    fun pinpointAccuracyForSideScaling() {
        val startMatrix = Matrix()
        val startTouch = Offset(100f, 50f) // Right handle local
        val currentTouch = Offset(150f, 60f) // Stretch right, also move down slightly

        // Handle 7 is Right
        val result = SelectionTransformMath.compute(startMatrix, bounds, 7, startTouch, currentTouch)

        // The X should match currentTouch.x, but Y should be ignored (horizontal scaling only)
        val transformedHandle = result.map(startTouch)
        assertEquals(150f, transformedHandle.x, 0.001f)
        assertEquals(50f, transformedHandle.y, 0.001f) // Y stays 50
    }

    @Test
    fun translationWorksIndependentlyOfRotation() {
        val startMatrix = Matrix().apply { rotateZ(45f) }
        val startTouch = Offset(50f, 50f)
        val currentTouch = Offset(70f, 80f)

        // Handle -1 is Translation
        val result = SelectionTransformMath.compute(startMatrix, bounds, -1, startTouch, currentTouch)

        val delta = result.map(Offset(0f, 0f)) - startMatrix.map(Offset(0f, 0f))
        assertEquals(20f, delta.x, 0.001f)
        assertEquals(30f, delta.y, 0.001f)
    }
}
