package com.donnclab.kinetiframe.domain.actor

import com.donnclab.kinetiframe.data.actor.ActorPlacement
import com.donnclab.kinetiframe.engine.model.ActorPlacementReference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ActorPlacementTimingTest {
    @Test fun `widening a trim restores all repeats with their source phase`() {
        val cut = ActorPlacementTrimming.shorten(placement, 19, 4)
        val restored = ActorPlacementTrimming.shorten(cut, 10, 24)
        for (frame in 10 until 34) {
            assertEquals(
                ActorPlacementTiming.elapsedMicros(placement, frame, 12),
                ActorPlacementTiming.elapsedMicros(restored, frame, 12),
            )
        }
    }

    @Test fun `trim retains authored time through a repeat boundary`() {
        val trimmed = ActorPlacementTrimming.shorten(placement, 19, 10)
        for (frame in 19 until 29) {
            assertEquals(
                ActorPlacementTiming.elapsedMicros(placement, frame, 12),
                ActorPlacementTiming.elapsedMicros(trimmed, frame, 12),
            )
        }
        assertNull(ActorPlacementTiming.elapsedMicros(trimmed, 29, 12))
    }

    @Test fun `successive trims and scene moves preserve source phase`() {
        val first = ActorPlacementTrimming.shorten(placement, 13, 20)
        val second = ActorPlacementTrimming.shorten(first, 15, 10)
        assertEquals(
            ActorPlacementTiming.elapsedMicros(placement, 15, 12),
            ActorPlacementTiming.elapsedMicros(second.copy(startFrame = 100), 100, 12),
        )
    }
    private val placement = ActorPlacementReference(
        placementId = "placement-wave",
        performanceId = "performance-wave",
        actorId = "actor-alice",
        actorRevisionId = "revision-1",
        label = "Alice · Wave",
        startFrame = 10,
        durationFrames = 12,
        repeatCount = 2,
    )

    @Test fun `returns no time before or after the placement`() {
        assertNull(ActorPlacementTiming.elapsedMicros(placement, 9, 12))
        assertNull(ActorPlacementTiming.elapsedMicros(placement, 34, 12))
    }

    @Test fun `resets local time at each placement repeat`() {
        assertEquals(0L, ActorPlacementTiming.elapsedMicros(placement, 10, 12))
        assertEquals(916_666L, ActorPlacementTiming.elapsedMicros(placement, 21, 12))
        assertEquals(0L, ActorPlacementTiming.elapsedMicros(placement, 22, 12))
    }

    @Test fun `durable placement follows the same repeat clock`() {
        val durable = ActorPlacement(
            placementId = "placement-wave",
            projectId = "project-1",
            performanceId = "performance-wave",
            startFrame = 10,
            durationFrames = 12,
            repeatCount = 2,
        )
        assertEquals(0L, ActorPlacementTiming.elapsedMicros(durable, 22, 12))
        assertNull(ActorPlacementTiming.elapsedMicros(durable, 34, 12))
    }
}
