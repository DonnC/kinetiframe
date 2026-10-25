package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.core.actor.motion.ActorPoseKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PerformanceAuthoringTest {
    @Test fun `converts sparse pose snapshots into deterministic per-part curves`() {
        val curves = PerformanceAuthoring.rotationCurves(
            listOf(
                ActorPoseKey(0, mapOf("head" to 0f, "arm" to 10f)),
                ActorPoseKey(500_000, mapOf("head" to 20f)),
                ActorPoseKey(1_000_000, mapOf("head" to 0f, "arm" to -10f)),
            ),
            durationMicros = 1_000_000,
        )

        assertEquals(listOf("arm", "head"), curves.map { it.boneId })
        assertEquals(listOf(0L, 1_000_000L), curves.first().points.map { it.timeMicros })
        assertEquals(listOf(0L, 500_000L, 1_000_000L), curves.last().points.map { it.timeMicros })
    }

    @Test fun `rejects duplicate key times before persistence`() {
        assertThrows(IllegalArgumentException::class.java) {
            PerformanceAuthoring.rotationCurves(
                listOf(ActorPoseKey(0, mapOf("head" to 0f)), ActorPoseKey(0, mapOf("head" to 1f))),
                durationMicros = 1_000_000,
            )
        }
    }
}
