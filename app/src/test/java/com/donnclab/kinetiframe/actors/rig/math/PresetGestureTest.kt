package com.donnclab.kinetiframe.actors.rig.math

import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplates
import com.donnclab.kinetiframe.actors.rig.model.SceneGraph
import com.donnclab.kinetiframe.actors.rig.presets.ActorMotionPreset
import com.donnclab.kinetiframe.actors.rig.presets.AppliedActorPreset
import com.donnclab.kinetiframe.actors.rig.presets.RotationChannel
import com.donnclab.kinetiframe.actors.rig.presets.RotationPoint
import com.donnclab.kinetiframe.engine.rigging.presets.PresetCurve
import com.donnclab.kinetiframe.project.model.StageDimensions
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class PresetGestureTest {
    private val root = listOf(
        File("src/main/assets/actor/presets/views"),
        File("app/src/main/assets/actor/presets/views"),
    ).first {
        it.isDirectory
    }
    private fun recipes() = root.listFiles()!!.filter {
        it.extension == "json"
    }.map { Json.decodeFromString<ActorMotionPreset>(it.readText()).validate() }
    private val rest = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions(1000, 800))
    private fun posed(preset: ActorMotionPreset, progress: Double): SceneGraph {
        val applied = AppliedActorPreset(preset)
        val offsets = applied.offsets((progress * applied.durationMicros).toLong())
        return rest.copy(
            bones = rest.bones.mapValues { (id, bone) ->
                bone.withRest(
                    localTransform = bone.localTransform.copy(
                        rotationDegrees = bone.localTransform.rotationDegrees + (offsets[id] ?: 0f),
                    ),
                )
            },
        )
    }

    @Test fun `authored channels never overshoot their segment or jump at knots`() {
        recipes().forEach { recipe ->
            assertTrue(recipe.schemaVersion in 2..3)
            recipe.channels.forEach { channel ->
                channel.points.zipWithNext().forEach { (a, b) ->
                    repeat(101) { i ->
                        val time = a.progress.toDouble() + (b.progress - a.progress) * i / 100.0
                        val value = PresetCurve.evaluate(channel.points, time, recipe.loop)
                        assertTrue(
                            value >= minOf(
                                a.degrees,
                                b.degrees,
                            ) - .001f && value <= maxOf(a.degrees, b.degrees) + .001f,
                            "${recipe.id}/${channel.boneId}: $value",
                        )
                    }
                }
                channel.points.drop(1).dropLast(1).forEach { key ->
                    // Short launch/contact segments have high curvature. Sample closer to the
                    // knot to measure its tangent, not two different finite-distance secants.
                    val e = .00001
                    val center = PresetCurve.evaluate(channel.points, key.progress.toDouble(), recipe.loop)
                    val left = (center - PresetCurve.evaluate(channel.points, key.progress - e, recipe.loop)) / e
                    val right = (PresetCurve.evaluate(channel.points, key.progress + e, recipe.loop) - center) / e
                    assertEquals(left, right, 1.0, "Velocity discontinuity: ${recipe.id}/${channel.boneId}")
                }
            }
        }
    }

    @Test fun `loops match velocity and pose at the seam`() {
        recipes().filter { it.loop }.forEach { recipe ->
            recipe.channels.forEach { channel ->
                val e = .00001
                val start = PresetCurve.evaluate(channel.points, 0.0, true)
                val finish = PresetCurve.evaluate(channel.points, 1.0, true)
                assertEquals(start, finish)
                val incoming = (finish - PresetCurve.evaluate(channel.points, 1 - e, true)) / e
                val outgoing = (PresetCurve.evaluate(channel.points, e, true) - start) / e
                assertEquals(incoming, outgoing, 1.0, "${recipe.id}/${channel.boneId}")
            }
        }
    }

    @Test fun `wave lifts hand above shoulder and returns to rest`() {
        val recipe = recipes().first { it.id == "front-wave" }
        val joints = RigGeometry.resolve(posed(recipe, .5))
        assertTrue(joints.getValue("right_hand").end.y < joints.getValue("right_upper_arm").start.y)
        assertTrue(AppliedActorPreset(recipe).offsets(3_000_000).values.all { it == 0f })
    }

    @Test fun `walk keeps foot orientation independent from parent hip and knee swings`() {
        val recipe = recipes().first { it.id == "profile-walk" }
        val baseline = RigGeometry.resolve(rest)
        repeat(101) { i ->
            val joints = RigGeometry.resolve(posed(recipe, i / 100.0))
            for (id in listOf(
                "left_foot",
                "right_foot",
            )) {
                assertEquals(baseline.getValue(id).angle, joints.getValue(id).angle, .01f)
            }
        }
    }

    @Test fun `invalid curve order nonfinite values and mismatched loop endpoints are rejected`() {
        val recipe = recipes().first { it.loop }
        fun invalid(points: List<RotationPoint>) =
            recipe.copy(channels = listOf(RotationChannel("head", points = points)))
        assertThrows(IllegalArgumentException::class.java) {
            invalid(listOf(RotationPoint(0f, 0f), RotationPoint(0f, 2f), RotationPoint(1f, 0f))).validate()
        }
        assertThrows(IllegalArgumentException::class.java) {
            invalid(listOf(RotationPoint(0f, Float.NaN), RotationPoint(1f, 0f))).validate()
        }
        assertThrows(IllegalArgumentException::class.java) {
            invalid(listOf(RotationPoint(0f, 0f), RotationPoint(1f, 2f))).validate()
        }
    }
}
