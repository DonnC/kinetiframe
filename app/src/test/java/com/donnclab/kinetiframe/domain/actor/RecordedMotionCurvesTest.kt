package com.donnclab.kinetiframe.domain.actor

import com.donnclab.kinetiframe.data.actor.RotationCurve
import com.donnclab.kinetiframe.data.actor.RotationCurvePoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecordedMotionCurvesTest {
    @Test fun `short gestures retain their destination and stationary samples are reduced`() {
        val points = listOf(RotationCurvePoint(0, 0f), RotationCurvePoint(20_000, 40f))
        val result = RecordedMotionCurves.merge(emptyList(), mapOf("part" to points), 0, 20_000, 100_000, .35f)
        assertEquals(40f, result.single().points.last().degrees)
        val stationary = List(100) { RotationCurvePoint(it * 50_000L, 5f) }
        assertEquals(2, RecordedMotionCurves.fit(stationary, .35f).size)
    }

    @Test fun `manual refinement preserves recorded samples and unrelated channels`() {
        val arm = RotationCurve("arm", points = listOf(RotationCurvePoint(0, 5f), RotationCurvePoint(1_000_000, 20f)))
        val head = arm.copy(boneId = "head")
        val result = PerformanceAuthoring.refineCurves(
            listOf(arm, head),
            com.donnclab.kinetiframe.engine.rigging.model.ActorPoseKey(500_000, mapOf("arm" to 30f)),
        )
        assertEquals(listOf(5f, 30f, 20f), result.first().points.map { it.degrees })
        assertSame(head, result.last())
    }

    @Test fun `layering retains untouched curves exactly and replaces only take interval`() {
        val head = RotationCurve("head", points = listOf(RotationCurvePoint(0, 5f), RotationCurvePoint(1_000_000, 20f)))
        val arm = head.copy(boneId = "arm")
        val result = RecordedMotionCurves.merge(
            listOf(head, arm),
            mapOf("arm" to listOf(RotationCurvePoint(200_000, 8f), RotationCurvePoint(500_000, 40f))),
            200_000,
            500_000,
            1_000_000,
            .35f,
        )
        assertSame(head, result.first { it.boneId == "head" })
        assertEquals(
            listOf(0L, 200_000L, 500_000L, 600_000L, 1_000_000L),
            result.first {
                it.boneId == "arm"
            }.points.map { it.timeMicros },
        )
    }

    @Test fun `angular jitter smoothing crosses wrap without reversing and retains endpoints`() {
        val points =
            listOf(RotationCurvePoint(0, 178f), RotationCurvePoint(50_000, -178f), RotationCurvePoint(100_000, -179f))
        val fitted = RecordedMotionCurves.fit(points, .75f)
        assertEquals(178f, fitted.first().degrees)
        assertEquals(-179f, fitted.last().degrees)
        assertTrue(kotlin.math.abs(fitted[1].degrees) > 175f)
        assertEquals(points, RecordedMotionCurves.fit(points, 0f))
    }

    @Test fun `capture refuses invalid or over budget samples rather than silently deleting motion`() {
        assertThrows(IllegalArgumentException::class.java) {
            RecordedMotionCurves.fit(listOf(RotationCurvePoint(0, Float.NaN)), .5f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            RecordedMotionCurves.fit(List(721) { RotationCurvePoint(it.toLong(), 0f) }, .5f)
        }
    }
}
