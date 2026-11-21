package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplates
import com.donnclab.kinetiframe.actors.rig.presets.oscillating
import com.donnclab.kinetiframe.actors.rig.presets.testRecipe
import com.donnclab.kinetiframe.core.actor.document.ActorRevision
import com.donnclab.kinetiframe.core.actor.document.PerformanceDocument
import com.donnclab.kinetiframe.core.actor.document.validate
import com.donnclab.kinetiframe.core.actor.motion.ActorPoseKey
import com.donnclab.kinetiframe.core.actor.presets.AppliedPresetDocument
import com.donnclab.kinetiframe.core.document.StageDimensions
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PerformanceCompatibilityTest {
    private val revision = ActorRevision(
        revisionId = "revision-a",
        actorId = "actor-a",
        sceneGraph = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd),
        createdAtEpochMs = 1,
    )

    @Test fun `unknown part is rejected against pinned revision`() {
        val performance =
            PerformanceDocument(
                performanceId = "performance-a", projectId = "project-a", actorId = "actor-a",
                actorRevisionId = "revision-a", displayName = "Wave", durationMicros = 1_000_000,
                poseKeys = listOf(
                    ActorPoseKey(0, mapOf("not-a-part" to 0f)),
                ),
                createdAtEpochMs = 1, updatedAtEpochMs = 1,
            )
        assertThrows(IllegalArgumentException::class.java) { PerformanceCompatibility.validate(performance, revision) }
    }

    private fun preset(vararg requiredBones: String) = AppliedPresetDocument(
        testRecipe("test-preset", requiredBones.map { oscillating(it, amplitude = 10f) }),
    )

    @Test fun `a saved preset snapshot referencing this revision's parts is accepted`() {
        val performance =
            PerformanceDocument(
                performanceId = "performance-a", projectId = "project-a", actorId = "actor-a",
                actorRevisionId = "revision-a", displayName = "Wave", durationMicros = 1_000_000,
                presetSnapshot = preset("torso", "head"), createdAtEpochMs = 1, updatedAtEpochMs = 1,
            )
        PerformanceCompatibility.validate(performance, revision) // must not throw
    }

    @Test fun `a saved preset snapshot needing a part this revision lacks is rejected`() {
        val performance =
            PerformanceDocument(
                performanceId = "performance-a", projectId = "project-a", actorId = "actor-a",
                actorRevisionId = "revision-a", displayName = "Wave", durationMicros = 1_000_000,
                presetSnapshot = preset("torso", "tail"), createdAtEpochMs = 1, updatedAtEpochMs = 1,
            )
        assertThrows(IllegalArgumentException::class.java) { PerformanceCompatibility.validate(performance, revision) }
    }
}
