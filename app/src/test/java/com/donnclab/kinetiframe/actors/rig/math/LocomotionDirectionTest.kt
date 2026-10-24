package com.donnclab.kinetiframe.actors.rig.math

import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplates
import com.donnclab.kinetiframe.actors.rig.presets.ActorMotionPreset
import com.donnclab.kinetiframe.actors.rig.presets.AppliedActorPreset
import com.donnclab.kinetiframe.core.actor.rigging.ActorView
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.core.geometry.Point2
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class LocomotionDirectionTest {
    private val assets = listOf(
        File("src/main/assets/actor/presets/views"),
        File("app/src/main/assets/actor/presets/views"),
    ).first {
        it.isDirectory
    }
    private val rest = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions(1000, 800), ActorView.PROFILE)
    private fun recipe(id: String) =
        Json.decodeFromString<ActorMotionPreset>(File(assets, "$id.json").readText()).validate()
    private fun joints(id: String, phase: Double): Map<String, RigGeometry.Joint> {
        val applied = AppliedActorPreset(recipe(id))
        val time = (phase * applied.durationMicros).toLong()
        val angles = applied.offsets(time)
        val translations = applied.rootTranslations(time, rest)
        return RigGeometry.resolve(
            rest.copy(
                bones = rest.bones.mapValues { (boneId, bone) ->
                    val move = translations[boneId]
                    bone.withRest(
                        localTransform = bone.localTransform.copy(
                            rotationDegrees =
                            bone.localTransform.rotationDegrees + (angles[boneId] ?: 0f),
                            translation = Point2(
                                bone.localTransform.x + (move?.x ?: 0f),
                                bone.localTransform.y + (move?.y ?: 0f),
                            ),
                        ),
                    )
                },
            ),
        )
    }

    @Test fun `right facing walk and run support sweep backwards while recovery travels forward`() {
        for (id in listOf("profile-walk", "profile-run")) {
            val contact = joints(id, 0.0)
            val oppositeContact = joints(id, .5)
            assertTrue(contact.getValue("right_foot").start.x > oppositeContact.getValue("right_foot").start.x, id)
            assertTrue(contact.getValue("left_foot").start.x < oppositeContact.getValue("left_foot").start.x, id)
            assertTrue(
                joints(id, .25).getValue("left_foot").start.y < joints(id, .25).getValue("right_foot").start.y - 20f,
                id,
            )
            assertTrue(
                joints(id, .75).getValue("right_foot").start.y < joints(id, .75).getValue("left_foot").start.y - 20f,
                id,
            )
            assertTrue(recipe(id).rootMotion.all { channel -> channel.points.all { it.x == 0f } })
        }
    }

    @Test fun `contact poses rest on the floor and sole remains level`() {
        val floor = RigGeometry.resolve(rest).getValue("right_foot").end.y
        for (id in listOf("profile-walk", "profile-run")) {
            assertEquals(floor, joints(id, 0.0).getValue("right_foot").end.y, .02f, id)
            assertEquals(floor, joints(id, .5).getValue("left_foot").end.y, .02f, id)
            repeat(101) { i ->
                val pose = joints(id, i / 100.0)
                for (side in listOf("left", "right")) {
                    assertEquals(
                        RigGeometry.resolve(rest).getValue("${side}_foot").angle,
                        pose.getValue("${side}_foot").angle,
                        .01f,
                        id,
                    )
                }
            }
        }
    }

    @Test fun `jump launches extended lands before absorbing and returns to rest`() {
        val id = "profile-jump"
        val floor = RigGeometry.resolve(rest).getValue("right_foot").end.y
        for (phase in listOf(0.0, .2, .32, .76, .84, 1.0)) {
            assertEquals(floor, joints(id, phase).getValue("right_foot").end.y, .02f, "phase $phase")
        }
        assertTrue(joints(id, .48).getValue("right_foot").end.y < floor - 100f)
        assertTrue(joints(id, .68).getValue("right_foot").end.y < floor - 40f)
        assertEquals(0f, AppliedActorPreset(recipe(id)).offsets(3_000_000)["right_shin"] ?: 0f)
    }
}
