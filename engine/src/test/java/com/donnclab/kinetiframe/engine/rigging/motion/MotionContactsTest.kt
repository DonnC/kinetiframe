// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.motion

import com.donnclab.kinetiframe.core.actor.motion.ContactBend
import com.donnclab.kinetiframe.core.actor.motion.ContactFollow
import com.donnclab.kinetiframe.core.actor.motion.JointContact
import com.donnclab.kinetiframe.core.actor.motion.RootTranslation
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.rigging.edit.PoseBend
import com.donnclab.kinetiframe.engine.rigging.edit.RigEditFixtures
import com.donnclab.kinetiframe.engine.rigging.edit.RigPoseEdits
import com.donnclab.kinetiframe.engine.rigging.math.RigidRigGeometry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MotionContactsTest {
    private val rest = RigEditFixtures.graph

    @Test fun `hand selection captures its parent endpoint and follow orientation`() {
        assertEquals("lower", MotionContacts.contactBone(rest, "hand"))
        assertEquals("lower", MotionContacts.contactBone(rest, "lower"))
        assertNull(MotionContacts.contactBone(rest, "root"))
        assertNull(MotionContacts.contactBone(rest, null))
        val contact = MotionContacts.captureContact(rest, "lower", "hand")
        assertEquals(Point2(140f, 20f), contact.position)
        assertEquals(ContactFollow("hand", 0f), contact.follow)
        assertSame(ContactBend.Positive, contact.bend)
    }

    @Test fun `contact application is deterministic across random seek order and preserves lengths`() {
        val contact = JointContact(Point2(80f, 80f), ContactBend.Negative, ContactFollow("hand", 25f))
        fun pose() = MotionPose.apply(
            rest,
            mapOf("upper" to 15f),
            mapOf("root" to RootTranslation(10f, 5f)),
            mapOf("lower" to contact),
        )
        val first = pose()
        MotionPose.apply(rest, mapOf("upper" to -80f), emptyMap(), emptyMap())
        assertEquals(first, pose())
        assertEquals(25f, RigidRigGeometry.resolve(first).getValue("hand").angle, .001f)
        rest.bones.forEach { (id, bone) -> assertEquals(bone.length, first.bones.getValue(id).length) }
        assertEquals(0f, rest.bones.getValue("root").localTransform.x)
    }

    @Test fun `rotation and root channels apply before contact solve while non root travel is ignored`() {
        val pose = MotionPose.apply(
            rest,
            mapOf("upper" to 20f),
            mapOf(
                "root" to RootTranslation(10f, 5f),
                "lower" to RootTranslation(100f, 100f),
            ),
            emptyMap(),
        )
        assertEquals(20f, pose.bones.getValue("upper").localTransform.rotationDegrees)
        assertEquals(Point2(10f, 5f), pose.bones.getValue("root").localTransform.translation)
        assertEquals(
            rest.bones.getValue("lower").localTransform.translation,
            pose.bones.getValue("lower").localTransform.translation,
        )
    }

    @Test fun `explicit joint pose controls contact bend but straight chains retain prior choice`() {
        val contacts = mapOf("lower" to JointContact(140f, 20f, -1))
        val emptyContacts = linkedMapOf<String, JointContact>()
        assertSame(emptyContacts, MotionContacts.followBendSide(rest, emptyContacts))
        assertEquals(-1, MotionContacts.followBendSide(rest, contacts).getValue("lower").bendSide)
        val posed = RigPoseEdits.pose(rest, "lower", Point2(80f, 80f), PoseBend.POSITIVE)
        assertEquals(1, MotionContacts.followBendSide(posed, contacts).getValue("lower").bendSide)
    }

    @Test fun `capture without an IK root has a typed failure`() {
        assertEquals(
            MotionContactIssue.MISSING_IK_ROOT,
            assertThrows(MotionContactException::class.java) {
                MotionContacts.captureContact(rest, "root", null)
            }.issue,
        )
    }
}
