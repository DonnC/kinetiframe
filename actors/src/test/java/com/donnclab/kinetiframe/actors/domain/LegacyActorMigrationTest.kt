package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplates
import com.donnclab.kinetiframe.actors.rig.presets.oscillating
import com.donnclab.kinetiframe.actors.rig.presets.testRecipe
import com.donnclab.kinetiframe.core.actor.motion.ActorPoseKey
import com.donnclab.kinetiframe.core.actor.presets.AppliedPresetDocument
import com.donnclab.kinetiframe.core.document.PuppetDefinition
import com.donnclab.kinetiframe.core.document.PuppetInstance
import com.donnclab.kinetiframe.core.document.StageDimensions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LegacyActorMigrationTest {
    @Test fun `legacy pose keys become a project performance`() {
        val definition =
            PuppetDefinition(
                "legacy-actor",
                "Alice",
                ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd),
            )
        val instance =
            PuppetInstance(
                "legacy-instance",
                "legacy-actor",
                poseKeys = listOf(ActorPoseKey(300_000, mapOf("head" to 10f))),
            )
        val performance = LegacyActorMigration.performance(
            instance,
            PerformanceIdentity("project-a", "actor-a", "revision-a", "performance-a", "Wave"),
            1,
        )
        assertEquals(1_000_000L, requireNotNull(performance).durationMicros)
        assertEquals(instance.poseKeys, performance.poseKeys)
        assertNull(
            LegacyActorMigration.performance(
                instance.copy(poseKeys = emptyList()),
                PerformanceIdentity("project-a", "actor-a", "revision-a", "performance-a", "Wave"),
                1,
            ),
        )
    }

    @Test fun `legacy procedural preset becomes an exact durable performance snapshot`() {
        val preset = AppliedPresetDocument(
            testRecipe("wave", listOf(oscillating("left_hand", amplitude = 20f)), durationSeconds = 2f),
            intensity = 1f,
            speed = 2f,
        )
        val instance = PuppetInstance("legacy-instance", "legacy-actor", motionPreset = preset)

        val migrated = requireNotNull(
            LegacyActorMigration.performance(
                instance,
                PerformanceIdentity("project-a", "actor-a", "revision-a", "performance-wave", "Wave"),
                1,
            ),
        )

        assertEquals(preset, migrated.presetSnapshot)
        assertEquals(1_000_000L, migrated.durationMicros)
    }
}
