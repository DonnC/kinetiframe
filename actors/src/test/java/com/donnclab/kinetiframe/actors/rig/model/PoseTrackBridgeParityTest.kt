package com.donnclab.kinetiframe.actors.rig.model

import com.donnclab.kinetiframe.core.actor.motion.ActorPoseKey
import com.donnclab.kinetiframe.core.actor.motion.JointContact
import com.donnclab.kinetiframe.core.actor.motion.RootTranslation
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.engine.rigging.math.CompiledMotionCurve
import com.donnclab.kinetiframe.engine.rigging.motion.PoseSampling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random

class PoseTrackBridgeParityTest {
    @Test fun `all interpolation modes match frozen sampling on human and animal rest revisions`() {
        val random = Random(113)
        listOf(ActorTemplate.HUMAN, ActorTemplate.ANIMAL).forEach { template ->
            val rest = ActorTemplates.create(template, StageDimensions.LandscapeHd)
            val keys = (0..4).map { index ->
                ActorPoseKey(
                    index * 250_000L,
                    rest.bones.keys.associateWith { random.nextFloat() * 360f - 180f },
                    rest.rootBoneIds.associateWith {
                        RootTranslation(random.nextFloat() * 50f, random.nextFloat() * 50f)
                    },
                )
            }.reversed()
            listOf(false, true).forEach { loop ->
                CompiledMotionCurve.Interpolation.entries.forEach { interpolation ->
                    val sampling = PoseSampling(loop, interpolation)
                    listOf(
                        -1L,
                        0L,
                        1L,
                        125_000L,
                        250_000L,
                        999_999L,
                        1_000_000L,
                        2_000_000L,
                    ).shuffled(random).forEach { time ->
                        assertEquals(
                            LegacyPoseTrackReference.evaluate(keys, time, rest, sampling),
                            ActorPoseTrack.evaluate(keys, time, rest, sampling),
                        )
                        assertEquals(
                            LegacyPoseTrackReference.rootTranslations(keys, time, rest, sampling),
                            ActorPoseTrack.rootTranslations(keys, time, rest, sampling),
                        )
                    }
                }
            }
        }
    }

    @Test fun `capture replacement and contact selection preserve former behavior`() {
        val rest = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd)
        val contacts = mapOf("left_shin" to JointContact(20f, 30f, -1))
        val expected = LegacyPoseTrackReference.capture(10, rest, rest, contacts)
        assertEquals(expected, ActorPoseTrack.capture(10, rest, rest, contacts))
        val keys = listOf(expected, ActorPoseKey(20, emptyMap()))
        listOf(9L, 10L, 19L, 20L, 100L).forEach { time ->
            assertEquals(LegacyPoseTrackReference.contacts(keys, time), ActorPoseTrack.contacts(keys, time))
        }
        val replacement = expected.copy(rotations = emptyMap())
        assertEquals(LegacyPoseTrackReference.put(keys, replacement), ActorPoseTrack.put(keys, replacement))
    }
}
