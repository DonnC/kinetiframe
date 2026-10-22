package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.data.ActorRevision
import com.donnclab.kinetiframe.actors.data.PerformanceDocument
import com.donnclab.kinetiframe.actors.data.validate
import com.donnclab.kinetiframe.actors.rig.model.ActorPoseKey
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplates
import com.donnclab.kinetiframe.project.model.StageDimensions
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

    private fun preset(vararg requiredBones: String) = com.donnclab.kinetiframe.actors.rig.presets.AppliedActorPreset(
        com.donnclab.kinetiframe.actors.rig.presets.ActorMotionPreset(
            id = "test-preset",
            name = "Test",
            description = "",
            durationSeconds = 1f,
            requiredBones = requiredBones.toList(),
            channels = requiredBones.map {
                com.donnclab.kinetiframe.actors.rig.presets.RotationChannel(boneId = it, amplitudeDegrees = 10f)
            },
        ),
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
