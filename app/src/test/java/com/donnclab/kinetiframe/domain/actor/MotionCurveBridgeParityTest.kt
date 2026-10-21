package com.donnclab.kinetiframe.domain.actor

import com.donnclab.kinetiframe.data.actor.CurveInterpolation
import com.donnclab.kinetiframe.data.actor.RootMotionCurve
import com.donnclab.kinetiframe.data.actor.RootMotionPoint
import com.donnclab.kinetiframe.data.actor.RotationCurve
import com.donnclab.kinetiframe.data.actor.RotationCurvePoint
import com.donnclab.kinetiframe.engine.rigging.motion.BeforeTake
import com.donnclab.kinetiframe.engine.rigging.motion.CurveSampling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MotionCurveBridgeParityTest {
    @Test fun `all modes duration policies and preset holds match frozen curve evaluators`() {
        CurveInterpolation.entries.forEach { mode ->
            val rotations =
                listOf(
                    RotationCurve(
                        "arm",
                        mode,
                        listOf(
                            RotationCurvePoint(0, 0f),
                            RotationCurvePoint(100, 50f),
                            RotationCurvePoint(200, -50f),
                            RotationCurvePoint(300, 0f),
                        ),
                    ),
                    RotationCurve("late", mode, listOf(RotationCurvePoint(100, 170f), RotationCurvePoint(250, -170f))),
                )
            val roots = rotations.map { curve ->
                RootMotionCurve(
                    curve.boneId,
                    curve.points.map { RootMotionPoint(it.timeMicros, it.degrees, -it.degrees) },
                    mode,
                )
            }
            listOf(false, true).forEach { loop ->
                BeforeTake.entries.forEach { before ->
                    listOf<Long?>(null, 300, 400).forEach { duration ->
                        val sampling = CurveSampling(loop, duration, before)
                        listOf(301L, -1L, 0L, 50L, 99L, 100L, 150L, 250L, 300L).forEach { time ->
                            assertEquals(
                                LegacyCurveSamplingReference.curveRotations(rotations, time, sampling),
                                PerformanceEvaluator.curveRotations(rotations, time, sampling),
                            )
                            if (duration !=
                                null
                            ) {
                                assertEquals(
                                    LegacyCurveSamplingReference.sample(roots, time, sampling),
                                    RootMotionEvaluator.sample(roots, time, sampling),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Test fun `malformed empty rotation channels keep original failure ordering`() {
        val empty = listOf(RotationCurve("arm", points = emptyList()))
        listOf(
            CurveSampling(),
            CurveSampling(loop = true),
            CurveSampling(beforeTake = BeforeTake.PRESERVE_PRESET),
        ).forEach { sampling ->
            val legacy =
                assertThrows(RuntimeException::class.java) {
                    LegacyCurveSamplingReference.curveRotations(empty, 0, sampling)
                }
            val current =
                assertThrows(RuntimeException::class.java) { PerformanceEvaluator.curveRotations(empty, 0, sampling) }
            assertEquals(legacy.javaClass, current.javaClass)
            assertEquals(legacy.message, current.message)
        }
    }
}
