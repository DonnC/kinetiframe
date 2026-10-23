package com.donnclab.kinetiframe.actors.rig.math

import com.donnclab.kinetiframe.actors.rig.model.ActorPoseTrack
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplates
import com.donnclab.kinetiframe.actors.rig.presets.ActorMotionPreset
import com.donnclab.kinetiframe.actors.rig.presets.AppliedActorPreset
import com.donnclab.kinetiframe.actors.rig.presets.RotationChannel
import com.donnclab.kinetiframe.core.actor.motion.ActorPoseKey
import com.donnclab.kinetiframe.core.document.StageDimensions
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ActorPresetTest {
    private fun recipes(): List<ActorMotionPreset> {
        val directory = File("src/main/assets/actor/presets").takeIf { it.isDirectory }
            ?: File("app/src/main/assets/actor/presets")
        val files = directory.walkTopDown().filter { it.isFile && it.extension == "json" }.toList()
        assertTrue(files.size >= 4)
        return files.map { Json.decodeFromString<ActorMotionPreset>(it.readText()).validate() }
    }

    @Test fun `all bundled recipes parse with unique ids and at least one compatible template`() {
        val presets = recipes()
        assertEquals(presets.size, presets.map { it.id }.toSet().size)
        val rigs = ActorTemplate.entries.map { ActorTemplates.create(it, StageDimensions.LandscapeHd) }
        presets.forEach { preset -> assertTrue(rigs.any { preset.missingBones(it.bones.keys).isEmpty() }, preset.id) }
    }

    @Test fun `continuous evaluation repeats loops without generating frames`() {
        recipes().filter { it.loop }.forEach { recipe ->
            val applied = AppliedActorPreset(recipe)
            val period = (recipe.durationSeconds * 1_000_000).toLong()
            val first = applied.offsets(123_456)
            val repeated = applied.offsets(period + 123_456)
            first.forEach { (id, value) -> assertEquals(value, repeated.getValue(id), 0.001f) }
            assertEquals(first, applied.offsets(123_456))
            assertEquals(applied.offsets(600_000), applied.copy(speed = 2f).offsets(300_000))
        }
    }

    @Test fun `one-shot bundled recipes settle and hold instead of repeating`() {
        recipes().filter { !it.loop }.forEach { recipe ->
            val applied = AppliedActorPreset(recipe)
            val durationMicros = (recipe.durationSeconds * 1_000_000).toLong()
            val atEnd = applied.offsets(durationMicros)
            val wellPastEnd = applied.offsets(durationMicros * 5)
            // A one-shot clip holds its final pose forever after -- unlike a loop, evaluating far
            // past its duration must not differ from evaluating at exactly its duration.
            atEnd.forEach { (id, value) -> assertEquals(value, wellPastEnd.getValue(id), 0.001f) }
            // The transient term has fully decayed by the clip's end, so every channel settles
            // to exactly its own offsetDegrees.
            recipe.channels.forEach { channel ->
                val value = if (recipe.schemaVersion == 1) {
                    channel.offsetDegrees
                } else {
                    channel.points.last().degrees -
                        channel.counterRotationBones.sumOf { id ->
                            recipe.channels.first { it.boneId == id }.points.last().degrees.toDouble()
                        }.toFloat()
                }
                assertEquals(value, atEnd.getValue(channel.boneId), 0.001f, channel.boneId)
            }
            // The doubled speed / halved time identity holds for one-shot recipes too, as long as
            // neither sample has already hit the clamp -- assert it against each recipe's own
            // still-in-flight midpoint rather than a fixed timestamp that might already be clamped
            // for a very short clip.
            val midpointMicros = durationMicros / 4
            assertEquals(applied.offsets(midpointMicros * 2), applied.copy(speed = 2f).offsets(midpointMicros))
        }
    }

    @Test fun `pose interpolation crosses angle boundary on shortest arc`() {
        val rest = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd)
        val keys = listOf(ActorPoseKey(0, mapOf("head" to 170f)), ActorPoseKey(1_000_000, mapOf("head" to -170f)))
        assertEquals(180f, ActorPoseTrack.evaluate(keys, 500_000, rest).getValue("head"), 0.001f)
        val replaced = ActorPoseTrack.put(keys, ActorPoseKey(0, mapOf("head" to 90f)))
        assertEquals(2, replaced.size)
        assertEquals(90f, replaced.first().rotations.getValue("head"))
    }

    @Test fun `unsupported schema and invalid durations fail validation`() {
        val recipe = recipes().first()
        assertThrows(IllegalArgumentException::class.java) { recipe.copy(schemaVersion = 99).validate() }
        assertThrows(IllegalArgumentException::class.java) { recipe.copy(durationSeconds = 0f).validate() }
        assertThrows(IllegalArgumentException::class.java) {
            recipe.copy(
                channels =
                recipe.channels + recipe.channels.first(),
            ).validate()
        }
    }

    // The tests below exercise AppliedActorPreset.offsets's one-shot math directly against a
    // small synthetic recipe, independent of any bundled JSON file -- so the shape of the formula
    // documented on that function is verified in isolation, not only through the values used by
    // the bundled presets.
    private fun oneShotRecipe(amplitude: Float = 40f, offset: Float = 30f, cycles: Float = 1f, phase: Float = 90f) =
        ActorMotionPreset(
            id = "test-one-shot",
            name = "Test",
            description = "",
            durationSeconds = 1f,
            loop = false,
            requiredBones = listOf("head"),
            channels = listOf(RotationChannel("head", offset, amplitude, cycles, phase)),
        )

    @Test fun `one-shot channel at time zero is pure transient with no held bias yet`() {
        val recipe = oneShotRecipe(amplitude = 40f, offset = 30f, phase = 90f)
        val value = AppliedActorPreset(recipe).offsets(0).getValue("head")
        // eased(0) == 0, so the offset contributes nothing; settle(0) == 1, so the full transient
        // (amplitude * sin(phaseDegrees), since progress == 0 leaves only the phase term) shows.
        assertEquals(40f * kotlin.math.sin(Math.toRadians(90.0)).toFloat(), value, 0.01f)
    }

    @Test fun `one-shot channel settles to its held offset and then holds`() {
        val recipe = oneShotRecipe(amplitude = 40f, offset = 30f, phase = 90f)
        val applied = AppliedActorPreset(recipe)
        val samples = (0..10).map { applied.offsets((it * 100_000).toLong()).getValue("head") }
        // The transient (currently 40 at t=0) must have fully decayed away by the end, leaving
        // exactly the held offset -- and once it reaches that value it must stop moving, since a
        // one-shot clip holds rather than continuing to animate past its duration.
        assertEquals(30f, samples.last(), 0.01f)
        assertEquals(samples.last(), applied.offsets(5_000_000).getValue("head"), 0.001f)
    }

    @Test fun `one-shot with zero offset returns exactly to the rig's rest pose`() {
        // This is the shape Utility (Stand Up) relies on: no held bias at all, so the channel
        // starts displaced (the crouched launch pose) and eases all the way back to 0 -- the
        // template's own rest rotation -- rather than to some new held pose.
        val recipe = oneShotRecipe(amplitude = 50f, offset = 0f, phase = 90f)
        val value = AppliedActorPreset(recipe).offsets(2_000_000).getValue("head")
        assertEquals(0f, value, 0.001f)
    }

    @Test fun `intensity scales a settled one-shot pose the same as a looping one`() {
        val recipe = oneShotRecipe(amplitude = 40f, offset = 30f, phase = 90f)
        val settled = AppliedActorPreset(recipe, intensity = 1.5f).offsets(5_000_000).getValue("head")
        assertEquals(30f * 1.5f, settled, 0.001f)
    }
}
