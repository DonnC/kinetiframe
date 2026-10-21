package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.engine.model.StageDimensions
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplate
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplates
import com.donnclab.kinetiframe.engine.rigging.model.RootTranslation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import kotlin.random.Random

class MotionPoseBridgeParityTest {
    @Test fun `seeded root rotation and contact application matches frozen pipeline graphs`() {
        val random = Random(112)
        listOf(ActorTemplate.HUMAN, ActorTemplate.ANIMAL).forEach { template ->
            val rest = ActorTemplates.create(template, StageDimensions.LandscapeHd)
            val endpoints = rest.bones.values.filter { it.ikChainRootId != null }.take(2)
            repeat(60) {
                val rotations = rest.bones.mapValues { (_, bone) ->
                    bone.localTransform.rotationDegrees +
                        random.nextFloat() * 60f -
                        30f
                }
                val roots = rest.rootBoneIds.associateWith {
                    RootTranslation(
                        random.nextFloat() * 20f - 10f,
                        random.nextFloat() * 20f - 10f,
                    )
                }
                val contacts = endpoints.associate { bone ->
                    val follow = rest.bones.values.firstOrNull { it.parentId == bone.id }?.id
                    val contact = LegacyMotionPoseReference.captureContact(rest, bone.id, follow)
                    bone.id to
                        contact.copy(position = contact.position.copy(x = contact.x + random.nextFloat() * 30f - 15f))
                }
                val expected = LegacyMotionPoseReference.apply(rest, rotations, roots, contacts)
                assertEquals(expected, ActorMotionPose.apply(rest, rotations, roots, contacts))
                assertEquals(
                    expected,
                    ActorMotionPose.apply(rest, rotations, roots, contacts.toList().reversed().toMap()),
                )
                assertEquals(
                    LegacyMotionPoseReference.followBendSide(expected, contacts),
                    ActorMotionPose.followBendSide(expected, contacts),
                )
                endpoints.forEach { bone ->
                    assertEquals(
                        LegacyMotionPoseReference.captureContact(expected, bone.id, null),
                        ActorMotionPose.captureContact(expected, bone.id, null),
                    )
                }
            }
        }
    }

    @Test fun `selection and missing IK capture diagnostics retain original behavior`() {
        val rest = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd)
        (rest.bones.keys + "missing").forEach { selected ->
            assertEquals(
                LegacyMotionPoseReference.contactBone(rest, selected),
                ActorMotionPose.contactBone(rest, selected),
            )
        }
        val root = rest.rootBoneIds.first()
        assertEquals(
            assertThrows(IllegalArgumentException::class.java) {
                LegacyMotionPoseReference.captureContact(rest, root, null)
            }.message,
            assertThrows(IllegalArgumentException::class.java) {
                ActorMotionPose.captureContact(rest, root, null)
            }.message,
        )
    }
}
