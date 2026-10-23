package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.data.PerformanceDocument
import com.donnclab.kinetiframe.core.document.ActorPlacementReference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ActorPlacementMapperTest {
    @Test fun `round trips an editor placement through its durable document`() {
        val reference = ActorPlacementReference(
            placementId = "placement-wave",
            performanceId = "performance-wave",
            actorId = "actor-alice",
            actorRevisionId = "revision-1",
            label = "Alice · Wave",
            startFrame = 4,
            durationFrames = 12,
        )
        val performance = PerformanceDocument(
            performanceId = "performance-wave",
            projectId = "project-film",
            actorId = "actor-alice",
            actorRevisionId = "revision-1",
            displayName = "Wave",
            durationMicros = 1_000_000,
            createdAtEpochMs = 1,
            updatedAtEpochMs = 1,
        )

        val document = ActorPlacementMapper.document("project-film", reference)
        assertEquals(reference, ActorPlacementMapper.reference(document, performance))
    }
}
