package com.donnclab.kinetiframe.engine.rigging.presets

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class PresetChannelValidationBridgeTest {
    private val channel = RotationChannel("head", points = listOf(RotationPoint(0f, 0f), RotationPoint(1f, 0f)))

    @Test fun `channel bridge preserves rejection order and messages across schemas and loop policies`() {
        val prototype = recipes().first().copy(rootMotion = emptyList())
        val sources = listOf(
            channel, RotationChannel("head"), channel.copy(boneId = ""),
            channel.copy(amplitudeDegrees = Float.NaN), channel.copy(amplitudeDegrees = 181f),
            channel.copy(phaseDegrees = 361f), channel.copy(points = emptyList()),
            channel.copy(points = listOf(RotationPoint(.1f, 0f), RotationPoint(1f, 0f))),
            channel.copy(points = listOf(RotationPoint(0f, Float.NaN), RotationPoint(1f, 0f))),
            channel.copy(points = listOf(RotationPoint(0f, 0f), RotationPoint(0f, 0f), RotationPoint(1f, 0f))),
            channel.copy(points = listOf(RotationPoint(0f, 0f), RotationPoint(1f, 1f))),
            channel.copy(
                counterRotationBones = listOf("missing"),
            ),
            channel.copy(counterRotationBones = listOf("head", "head")),
        )
        listOf(1, 2, 3).forEach { schema ->
            listOf(false, true).forEach { loop ->
                sources.forEach { source ->
                    val channels = listOf(source)
                    val expected = runCatching {
                        LegacyPresetChannelValidationReference.validate(channels, schema, loop)
                    }.exceptionOrNull()
                    val actual = runCatching {
                        prototype.copy(schemaVersion = schema, loop = loop, channels = channels).validate()
                    }.exceptionOrNull()
                    assertEquals(expected?.javaClass, actual?.javaClass)
                    assertEquals(expected?.message, actual?.message)
                }
            }
        }
    }

    @Test fun `shipped channel snapshots validate after persisted roundtrip`() {
        recipes().forEach { recipe ->
            val restored = Json.decodeFromString(
                ActorMotionPreset.serializer(),
                Json.encodeToString(ActorMotionPreset.serializer(), recipe),
            ).validate()
            assertEquals(recipe, restored)
        }
    }

    private fun recipes(): List<ActorMotionPreset> {
        val directory = File("src/main/assets/actor/presets").takeIf { it.isDirectory }
            ?: File("app/src/main/assets/actor/presets")
        return directory.walkTopDown().filter { it.isFile && it.extension == "json" }
            .map { Json.decodeFromString<ActorMotionPreset>(it.readText()).validate() }.toList()
    }
}
