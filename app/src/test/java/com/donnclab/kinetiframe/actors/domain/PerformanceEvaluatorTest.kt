package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.data.PerformanceDocument
import com.donnclab.kinetiframe.actors.data.PerformancePlayback
import com.donnclab.kinetiframe.actors.data.RotationCurve
import com.donnclab.kinetiframe.actors.data.RotationCurvePoint
import com.donnclab.kinetiframe.actors.data.validate
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplates
import com.donnclab.kinetiframe.project.model.StageDimensions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PerformanceEvaluatorTest {
    @Test fun `a later recorded override does not freeze the earlier procedural gesture`() {
        val recipe = com.donnclab.kinetiframe.actors.rig.presets.ActorMotionPreset(
            id = "idle",
            name = "Idle",
            description = "",
            durationSeconds = 2f,
            requiredBones = listOf("head"),
            channels = listOf(
                com.donnclab.kinetiframe.actors.rig.presets.RotationChannel("head", amplitudeDegrees = 12f),
            ),
        )
        val preset = com.donnclab.kinetiframe.actors.rig.presets.AppliedActorPreset(recipe)
        val document = performance().copy(
            durationMicros = 5_000_000,
            presetSnapshot = preset,
            rotationCurves = listOf(
                RotationCurve(
                    "head",
                    points = listOf(RotationCurvePoint(3_000_000, 20f), RotationCurvePoint(4_000_000, 40f)),
                ),
            ),
        )
        val time = 500_000L
        val expected =
            rest.bones.getValue("head").localTransform.rotationDegrees + preset.offsets(time).getValue("head")
        assertEquals(expected, PerformanceEvaluator.rotations(document, time, rest).getValue("head"), .001f)
    }

    @Test fun `legacy preset containers do not reset at an unrelated duration or restart a one shot`() {
        val recipe = com.donnclab.kinetiframe.actors.rig.presets.ActorMotionPreset(
            id = "legacy",
            name = "Legacy",
            description = "",
            durationSeconds = 1.8f,
            requiredBones = listOf("head"),
            channels = listOf(
                com.donnclab.kinetiframe.actors.rig.presets.RotationChannel("head", amplitudeDegrees = 12f),
            ),
        )
        val applied = com.donnclab.kinetiframe.actors.rig.presets.AppliedActorPreset(recipe)
        val document = performance().copy(durationMicros = 2_000_000, presetSnapshot = applied)
        val base = rest.bones.getValue("head").localTransform.rotationDegrees
        for (time in listOf(1_999_999L, 2_000_000L, 2_000_001L, 10_000_000L)) {
            assertEquals(
                base + applied.offsets(time).getValue("head"),
                PerformanceEvaluator.rotations(document, time, rest).getValue("head"),
                .001f,
            )
        }
        val oneShot = applied.copy(
            recipe = recipe.copy(
                loop = false,
                channels = listOf(
                    com.donnclab.kinetiframe.actors.rig.presets.RotationChannel("head", offsetDegrees = 30f),
                ),
            ),
        )
        val saved = document.copy(presetSnapshot = oneShot)
        assertEquals(base + 30f, PerformanceEvaluator.rotations(saved, 2_000_000L, rest).getValue("head"), .001f)
        assertEquals(base + 30f, PerformanceEvaluator.rotations(saved, 10_000_000L, rest).getValue("head"), .001f)
    }

    private val rest = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd)

    @Test fun `linear curve takes shortest angular path`() {
        val document = performance().copy(
            rotationCurves = listOf(
                RotationCurve(
                    "head",
                    points = listOf(
                        RotationCurvePoint(0, 170f),
                        RotationCurvePoint(1_000, -170f),
                    ),
                ),
            ),
        )
        assertEquals(180f, PerformanceEvaluator.rotations(document, 500, rest).getValue("head"), 0.001f)
    }

    @Test fun `loop wraps while end hold clamps`() {
        val loop = performance().copy(playback = PerformancePlayback.LOOP)
        val hold = performance().copy(playback = PerformancePlayback.END_HOLD)
        assertEquals(100L, PerformanceEvaluator.normalizeTime(loop, 1_100))
        assertEquals(1_000L, PerformanceEvaluator.normalizeTime(hold, 1_100))
    }

    @Test fun `schema one documents remain valid`() {
        performance().copy(schemaVersion = 1).validate()
    }

    @Test fun `a saved preset snapshot renders exactly like the applied preset it was saved from`() {
        // This is the end-to-end check that a preset saved through ActorStudioDocumentViewModel
        // actually plays back: rotations() must fold presetSnapshot's offsets onto the rig's rest
        // pose, on top of (not replacing) any recorded pose keys or rotation curves.
        val preset = com.donnclab.kinetiframe.actors.rig.presets.AppliedActorPreset(
            com.donnclab.kinetiframe.actors.rig.presets.ActorMotionPreset(
                id = "human-cheer",
                name = "Cheer",
                description = "",
                durationSeconds = 2f,
                requiredBones = listOf("torso"),
                channels = listOf(
                    com.donnclab.kinetiframe.actors.rig.presets.RotationChannel(
                        boneId = "torso",
                        amplitudeDegrees = 15f,
                    ),
                ),
            ),
        )
        val document = performance().copy(durationMicros = 2_000_000, presetSnapshot = preset)
        val restTorsoDegrees = rest.bones.getValue("torso").localTransform.rotationDegrees
        val expected = restTorsoDegrees + preset.offsets(500_000).getValue("torso")
        assertEquals(expected, PerformanceEvaluator.rotations(document, 500_000, rest).getValue("torso"), 0.001f)
    }

    private fun performance() = PerformanceDocument(
        performanceId = "performance-wave",
        projectId = "project-film",
        actorId = "actor-alice",
        actorRevisionId = "revision-alice",
        displayName = "Wave",
        durationMicros = 1_000,
        createdAtEpochMs = 1,
        updatedAtEpochMs = 1,
    )
}
