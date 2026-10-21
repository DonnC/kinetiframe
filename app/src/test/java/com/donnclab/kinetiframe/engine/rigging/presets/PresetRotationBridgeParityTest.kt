package com.donnclab.kinetiframe.engine.rigging.presets

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class PresetRotationBridgeParityTest {
    @Test fun `legacy loop and one shot preserve exact former formulas across settings and extreme times`() {
        val recipe = recipes().first().copy(
            schemaVersion = 1,
            durationSeconds = 1.7f,
            channels = listOf(
                RotationChannel(
                    "head",
                    offsetDegrees = 21f,
                    amplitudeDegrees = -38f,
                    cyclesPerLoop = 1.3f,
                    phaseDegrees = 35f,
                ),
            ),
        )
        listOf(false, true).forEach { loop ->
            listOf(-1f, .25f, 1f, 3f, 9f).forEach { speed ->
                listOf(-1f, 0f, .7f, 2f, 5f).forEach { intensity ->
                    compare(AppliedActorPreset(recipe.copy(loop = loop), intensity, speed))
                }
            }
        }
    }

    @Test fun `all shipped recipes retain evaluated parent compensation and sample order`() {
        recipes().forEach { recipe ->
            listOf(.25f, 1f, 3f).forEach { speed ->
                listOf(0f, .7f, 2f).forEach { intensity -> compare(AppliedActorPreset(recipe, intensity, speed)) }
            }
        }
    }

    @Test fun `lazy adaptation does not add persisted state or survive copy with stale settings`() {
        val applied = AppliedActorPreset(recipes().first(), .7f, 1.3f)
        val encoded = Json.encodeToString(applied)
        val defaults = Json { encodeDefaults = true }
        val full = defaults.encodeToString(applied)
        assertEquals(setOf("recipe", "intensity", "speed"), Json.parseToJsonElement(full).jsonObject.keys)
        applied.offsets(123_456)
        assertEquals(encoded, Json.encodeToString(applied))
        assertEquals(full, defaults.encodeToString(applied))
        val restored = Json.decodeFromString<AppliedActorPreset>(encoded)
        assertEquals(applied.offsets(123_456), restored.offsets(123_456))
        val changed = applied.copy(intensity = 2f, speed = .25f)
        assertEquals(LegacyPresetRotationReference.offsets(changed, 123_456), changed.offsets(123_456))
        val replaced = applied.copy(recipe = applied.recipe.copy(channels = emptyList()))
        assertEquals(emptyMap<String, Float>(), replaced.offsets(123_456))
    }

    private fun compare(applied: AppliedActorPreset) {
        listOf(
            Long.MIN_VALUE, -1L, 0L, 1L, 123_456L, applied.durationMicros / 2,
            applied.durationMicros, applied.durationMicros * 4, Long.MAX_VALUE,
        ).forEach { time ->
            val expected = LegacyPresetRotationReference.offsets(applied, time)
            val actual = applied.offsets(time)
            assertEquals(expected, actual, "${applied.recipe.id}/$time/${applied.speed}/${applied.intensity}")
            assertEquals(expected.keys.toList(), actual.keys.toList())
        }
    }

    private fun recipes(): List<ActorMotionPreset> {
        val directory = File("src/main/assets/actor/presets").takeIf { it.isDirectory }
            ?: File("app/src/main/assets/actor/presets")
        return directory.walkTopDown().filter { it.isFile && it.extension == "json" }
            .map { Json.decodeFromString<ActorMotionPreset>(it.readText()).validate() }.toList()
    }
}
