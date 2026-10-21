// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.motion

import com.donnclab.kinetiframe.core.actor.motion.ActorPoseKey
import com.donnclab.kinetiframe.core.actor.motion.JointContact
import com.donnclab.kinetiframe.core.actor.motion.RootTranslation
import com.donnclab.kinetiframe.engine.rigging.edit.RigEditFixtures
import com.donnclab.kinetiframe.engine.rigging.math.CompiledMotionCurve
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CompiledPoseTrackTest {
    private val rest = RigEditFixtures.graph

    @Test fun `empty tracks sample empty and missing channels retain rest and zero root defaults`() {
        val empty = CompiledPoseTrack(emptyList(), rest)
        assertTrue(empty.rotations(0).isEmpty())
        assertTrue(empty.rootTranslations(0).isEmpty())
        val track = CompiledPoseTrack(listOf(ActorPoseKey(5, emptyMap())), rest)
        assertEquals(rest.bones.mapValues { it.value.localTransform.rotationDegrees }, track.rotations(100))
        assertEquals(rest.rootBoneIds.associateWith { RootTranslation() }, track.rootTranslations(100))
    }

    @Test fun `compilation sorts once and samples shortest rotation arc and root travel`() {
        val keys = listOf(
            ActorPoseKey(1000, mapOf("upper" to -170f), mapOf("root" to RootTranslation(10f, 20f))),
            ActorPoseKey(0, mapOf("upper" to 170f)),
        )
        val track = CompiledPoseTrack(keys, rest)
        assertEquals(180f, track.rotations(500, CompiledMotionCurve.Interpolation.LINEAR).getValue("upper"))
        assertEquals(
            RootTranslation(5f, 10f),
            track.rootTranslations(500, CompiledMotionCurve.Interpolation.LINEAR).getValue("root"),
        )
        assertEquals(170f, track.rotations(-1).getValue("upper"))
        assertEquals(-170f, track.rotations(1001).getValue("upper"))
    }

    @Test fun `compiled channels snapshot input values and duplicate times retain rejection`() {
        val rotations = mutableMapOf("upper" to 20f)
        val keys = mutableListOf(ActorPoseKey(0, rotations))
        val track = CompiledPoseTrack(keys, rest)
        rotations["upper"] = 80f
        keys.clear()
        assertEquals(20f, track.rotations(0).getValue("upper"))
        assertThrows(IllegalArgumentException::class.java) {
            CompiledPoseTrack(listOf(ActorPoseKey(0, emptyMap()), ActorPoseKey(0, emptyMap())), rest)
        }
    }

    @Test fun `contacts hold until explicit release without changing legacy list order`() {
        val held = mapOf("lower" to JointContact(80f, 80f, -1))
        val keys = listOf(ActorPoseKey(0, emptyMap(), contacts = held), ActorPoseKey(100, emptyMap()))
        assertTrue(PoseKeys.contacts(keys, -1).isEmpty())
        assertEquals(held, PoseKeys.contacts(keys, 99))
        assertTrue(PoseKeys.contacts(keys, 100).isEmpty())
        assertEquals(held, PoseKeys.contacts(keys.reversed(), 100))
        assertEquals(
            listOf(0L, 100L),
            PoseKeys.put(keys.reversed(), ActorPoseKey(0, mapOf("upper" to 25f))).map {
                it.timeMicros
            },
        )
    }

    @Test fun `capture records root delta against rest without changing source graph`() {
        val posed = rest.copy(
            bones = rest.bones + (
                "root" to rest.bones.getValue(
                    "root",
                ).withRest(localTransform = rest.bones.getValue("root").localTransform.copy(rotationDegrees = 15f))
                ),
        )
        val key = PoseKeys.capture(50, posed, rest)
        assertEquals(15f, key.rotations.getValue("root"))
        assertEquals(RootTranslation(), key.rootTranslations.getValue("root"))
        assertEquals(0f, rest.bones.getValue("root").localTransform.rotationDegrees)
    }
}
