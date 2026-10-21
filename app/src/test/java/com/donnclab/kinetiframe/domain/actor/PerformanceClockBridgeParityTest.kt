package com.donnclab.kinetiframe.domain.actor

import com.donnclab.kinetiframe.data.actor.PerformanceDocument
import com.donnclab.kinetiframe.data.actor.PerformancePlayback
import com.donnclab.kinetiframe.data.actor.RootMotionCurve
import com.donnclab.kinetiframe.data.actor.RootMotionPoint
import com.donnclab.kinetiframe.data.actor.RotationCurve
import com.donnclab.kinetiframe.data.actor.RotationCurvePoint
import com.donnclab.kinetiframe.engine.model.StageDimensions
import com.donnclab.kinetiframe.engine.rigging.math.ActorMotionPose
import com.donnclab.kinetiframe.engine.rigging.model.ActorArtworkKey
import com.donnclab.kinetiframe.engine.rigging.model.ActorPoseKey
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplate
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplates
import com.donnclab.kinetiframe.engine.rigging.model.RootTranslation
import com.donnclab.kinetiframe.engine.rigging.model.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.presets.ActorMotionPreset
import com.donnclab.kinetiframe.engine.rigging.presets.AppliedActorPreset
import com.donnclab.kinetiframe.engine.rigging.presets.RootMotionChannel
import com.donnclab.kinetiframe.engine.rigging.presets.RotationChannel
import com.donnclab.kinetiframe.engine.rigging.presets.RotationPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import com.donnclab.kinetiframe.engine.rigging.presets.RootMotionPoint as PresetRootPoint

class PerformanceClockBridgeParityTest {
    @Test fun `clocks rotations and complete poses match frozen document evaluation`() {
        listOf(ActorTemplate.HUMAN, ActorTemplate.ANIMAL).forEach { template ->
            val rest = ActorTemplates.create(template, StageDimensions.LandscapeHd)
            listOf(false, true).forEach { loop ->
                val cases = documents(rest, loop)
                PerformancePlayback.entries.forEach { mode ->
                    cases.forEach { source ->
                        val document = source.copy(playback = mode)
                        SAMPLE_TIMES.forEach { time ->
                            assertEquals(
                                LegacyPerformanceReference.normalizeTime(document, time),
                                PerformanceEvaluator.normalizeTime(document, time),
                            )
                            assertEquals(
                                LegacyPerformanceReference.rotations(document, time, rest),
                                PerformanceEvaluator.rotations(document, time, rest),
                            )
                            assertEquals(
                                LegacyPerformanceReference.pose(document, time, rest),
                                PerformanceEvaluator.pose(document, time, rest),
                            )
                        }
                    }
                }
            }
        }
    }

    @Test fun `rotation only evaluation does not compile unrelated malformed root channels`() {
        val rest = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd)
        val document = documents(
            rest,
            true,
        ).first().copy(rootMotionCurves = listOf(RootMotionCurve("torso", emptyList())))
        assertEquals(
            LegacyPerformanceReference.rotations(document, -1, rest),
            PerformanceEvaluator.rotations(document, -1, rest),
        )
        val expected = assertThrows(RuntimeException::class.java) { LegacyPerformanceReference.pose(document, 0, rest) }
        val actual = assertThrows(RuntimeException::class.java) { PerformanceEvaluator.pose(document, 0, rest) }
        assertEquals(expected.javaClass, actual.javaClass)
        assertEquals(expected.message, actual.message)
    }

    private fun documents(rest: SceneGraph, loop: Boolean): List<PerformanceDocument> {
        val root = rest.rootBoneIds.first()
        val recipe =
            ActorMotionPreset(
                schemaVersion = 3, id = "clock-test", name = "Clock", description = "", durationSeconds = .8f,
                requiredBones = listOf(root),
                channels = listOf(
                    RotationChannel(
                        root,
                        points = listOf(
                            RotationPoint(0f, 0f),
                            RotationPoint(.5f, 15f),
                            RotationPoint(1f, 0f),
                        ),
                    ),
                ),
                loop = loop,
                rootMotion = listOf(
                    RootMotionChannel(
                        root,
                        listOf(PresetRootPoint(0f), PresetRootPoint(.5f, .1f, -.1f), PresetRootPoint(1f)),
                    ),
                ),
            )
        val base =
            PerformanceDocument(
                performanceId = "performance", projectId = "project", actorId = "actor", actorRevisionId = "revision",
                displayName = "Clock", durationMicros = 2_000_000,
                presetSnapshot = AppliedActorPreset(
                    recipe,
                    speed = 1.25f,
                ),
                createdAtEpochMs = 1, updatedAtEpochMs = 1,
            )
        val endpoint = rest.bones.values.firstOrNull { it.ikChainRootId != null }?.id
        val contacts = endpoint?.let { mapOf(it to ActorMotionPose.captureContact(rest, it, null)) }.orEmpty()
        val keys = listOf(
            ActorPoseKey(0, mapOf(root to 20f), mapOf(root to RootTranslation()), contacts),
            ActorPoseKey(2_000_000, mapOf(root to 20f), mapOf(root to RootTranslation())),
        )
        val rotations =
            listOf(
                RotationCurve(
                    root,
                    points = listOf(RotationCurvePoint(1_000_000, 30f), RotationCurvePoint(2_000_000, 0f)),
                ),
            )
        val roots =
            listOf(
                RootMotionCurve(root, listOf(RootMotionPoint(1_000_000, 20f, 10f), RootMotionPoint(2_000_000, 0f, 0f))),
            )
        val artwork = listOf(ActorArtworkKey(0, emptyMap()))
        return listOf(
            base,
            base.copy(poseKeys = keys),
            base.copy(rotationCurves = rotations),
            base.copy(rootMotionCurves = roots),
            base.copy(artworkKeys = artwork),
            base.copy(poseKeys = keys, rotationCurves = rotations, rootMotionCurves = roots, artworkKeys = artwork),
            base.copy(presetSnapshot = null, poseKeys = keys, rotationCurves = rotations, rootMotionCurves = roots),
        )
    }
}

private val SAMPLE_TIMES =
    listOf(Long.MIN_VALUE, -1L, 0L, 400_000L, 640_000L, 1_999_999L, 2_000_000L, 2_000_001L, Long.MAX_VALUE)
