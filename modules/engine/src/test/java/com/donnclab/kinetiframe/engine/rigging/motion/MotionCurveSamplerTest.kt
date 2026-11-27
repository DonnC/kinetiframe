// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.motion

import com.donnclab.kinetiframe.core.actor.motion.CurveInterpolation
import com.donnclab.kinetiframe.core.actor.motion.RootMotionCurve
import com.donnclab.kinetiframe.core.actor.motion.RootMotionPoint
import com.donnclab.kinetiframe.core.actor.motion.RootTranslation
import com.donnclab.kinetiframe.core.actor.motion.RotationCurve
import com.donnclab.kinetiframe.core.actor.motion.RotationCurvePoint
import com.donnclab.kinetiframe.engine.rigging.math.CompiledMotionCurve
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MotionCurveSamplerTest {
    private val sampler = MotionCurveSampler()
    private val rotation =
        RotationCurve("arm", points = listOf(RotationCurvePoint(100, 170f), RotationCurvePoint(300, -170f)))
    private val root =
        RootMotionCurve(
            "root",
            listOf(RootMotionPoint(100, 170f, -40f), RootMotionPoint(300, -170f, 40f)),
            CurveInterpolation.LINEAR,
        )

    @Test fun `angles follow shortest arc but root displacement never wraps`() {
        assertEquals(180f, sampler.rotations(listOf(rotation), 200).getValue("arm"))
        assertEquals(RootTranslation(), sampler.rootTranslations(listOf(root), 200).getValue("root"))
        assertEquals(170f, sampler.rotations(listOf(rotation), 0).getValue("arm"))
        assertEquals(-170f, sampler.rotations(listOf(rotation), 400).getValue("arm"))
    }

    @Test fun `preset preservation skips only before the authored first point`() {
        val sampling = CurveSampling(beforeTake = BeforeTake.PRESERVE_PRESET)
        assertTrue(sampler.rotations(listOf(rotation), 99, sampling).isEmpty())
        assertTrue(sampler.rootTranslations(listOf(root), 99, sampling).isEmpty())
        assertEquals(170f, sampler.rotations(listOf(rotation), 100, sampling).getValue("arm"))
        assertEquals(
            RootTranslation(170f, -40f),
            sampler.rootTranslations(listOf(root), 100, sampling).getValue("root"),
        )
    }

    @Test fun `rotation can infer a periodic end but roots require the enclosing duration`() {
        val points =
            listOf(
                RotationCurvePoint(0, 0f),
                RotationCurvePoint(100, 20f),
                RotationCurvePoint(200, -20f),
                RotationCurvePoint(300, 0f),
            )
        val curve = RotationCurve("arm", CurveInterpolation.SMOOTH, points)
        val travel = RootMotionCurve("root", points.map { RootMotionPoint(it.timeMicros, it.degrees, 0f) })
        val loop = CurveSampling(loop = true)
        val full = loop.copy(durationMicros = 300)
        assertEquals(sampler.rotations(listOf(curve), 50, full), sampler.rotations(listOf(curve), 50, loop))
        assertNotEquals(
            sampler.rootTranslations(listOf(travel), 50, full),
            sampler.rootTranslations(listOf(travel), 50, loop),
        )
        assertEquals(
            sampler.rootTranslations(listOf(travel), 50),
            sampler.rootTranslations(listOf(travel), 50, full.copy(durationMicros = 400)),
        )
    }

    @Test fun `all interpolation modes map explicitly and duplicate channel ids retain last value`() {
        CurveInterpolation.entries.forEach { mode ->
            assertEquals(mode.name, MotionInterpolation.curve(mode).name)
            val source = rotation.copy(interpolation = mode)
            val expected = CompiledMotionCurve(
                source.points.map {
                    it.timeMicros
                },
                source.points.map { it.degrees },
                angular = true,
            )
                .sample(150, MotionInterpolation.curve(mode))
            assertEquals(expected, sampler.rotations(listOf(source), 150).getValue("arm"))
        }
        val replacement = rotation.copy(points = listOf(RotationCurvePoint(0, 30f)))
        assertEquals(30f, sampler.rotations(listOf(rotation, replacement), 200).getValue("arm"))
    }

    @Test fun `empty channels and invalid curves retain raw rejection behavior`() {
        assertTrue(sampler.rotations(emptyList(), 0).isEmpty())
        assertTrue(sampler.rootTranslations(emptyList(), 0).isEmpty())
        val empty = rotation.copy(points = emptyList())
        assertThrows(IllegalArgumentException::class.java) { sampler.rotations(listOf(empty), 0) }
        assertThrows(NoSuchElementException::class.java) {
            sampler.rotations(listOf(empty), 0, CurveSampling(loop = true))
        }
        assertThrows(NoSuchElementException::class.java) {
            sampler.rotations(listOf(empty), 0, CurveSampling(beforeTake = BeforeTake.PRESERVE_PRESET))
        }
        assertThrows(IllegalArgumentException::class.java) {
            sampler.rotations(listOf(rotation.copy(points = listOf(RotationCurvePoint(0, Float.NaN)))), 0)
        }
    }

    @Test fun `clear and isolated samplers retain deterministic results with bounded limits`() {
        val small = MotionCurveSampler(CurveCacheLimits(1, 1))
        val expected = small.rotations(listOf(rotation), 200)
        repeat(5) { value -> small.rotations(listOf(rotation.copy(boneId = value.toString())), 150) }
        assertEquals(expected, small.rotations(listOf(rotation.copy(points = rotation.points.toList())), 200))
        small.clear()
        assertEquals(expected, small.rotations(listOf(rotation), 200))
        assertEquals(expected, MotionCurveSampler().rotations(listOf(rotation), 200))
        assertThrows(CompilationCacheException::class.java) { MotionCurveSampler(CurveCacheLimits(0, 1)) }
    }
}
