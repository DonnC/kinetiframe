package com.donnclab.kinetiframe.actors.rig.presets

import com.donnclab.kinetiframe.core.actor.presets.AppliedPresetDocument
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.round

@OptIn(ExperimentalSerializationApi::class)
class AppliedPresetCodecBridgeTest {
    private val formats = listOf(Json, Json { encodeDefaults = true })
    private val recipe = ActorMotionPreset(
        id = "test",
        name = "Test",
        description = "",
        durationSeconds = 1.7f,
        requiredBones = listOf("head"),
        channels = listOf(RotationChannel("head")),
    )

    @Test fun `neutral application matches generated wire fields defaults and raw settings`() {
        for (intensity in listOf(-1f, -0f, 0f, 1f, 2f)) {
            for (speed in listOf(-1f, -0f, .25f, 1f, 1.3f, 3f, 9f)) {
                val applied = AppliedActorPreset(recipe, intensity, speed)
                val document = AppliedPresetDocument(recipe.document(), intensity, speed)
                formats.forEach { json ->
                    val wire = json.encodeToString(applied)
                    assertEquals(wire, json.encodeToString(document))
                    assertEquals(document, json.decodeFromString<AppliedPresetDocument>(wire))
                    assertEquals(applied, json.decodeFromString<AppliedActorPreset>(wire))
                }
            }
        }
        val old = AppliedActorPreset.serializer().descriptor
        val neutral = AppliedPresetDocument.serializer().descriptor
        assertEquals(old.serialName, neutral.serialName)
        assertEquals(old.elementsCount, neutral.elementsCount)
        repeat(old.elementsCount) { index ->
            assertEquals(old.getElementName(index), neutral.getElementName(index))
            assertEquals(old.isElementOptional(index), neutral.isElementOptional(index))
        }
    }

    @Test fun `product duration retains frozen formula without serializing runtime state`() {
        listOf(-1f, 0f, .25f, 1.7f, 30f, Float.MAX_VALUE, Float.POSITIVE_INFINITY, Float.NaN).forEach { duration ->
            listOf(-1f, .25f, 1f, 1.3f, 3f, 9f, Float.NaN).forEach { speed ->
                val applied = AppliedActorPreset(recipe.copy(durationSeconds = duration), speed = speed)
                val expected = round(
                    duration.toDouble() * 1_000_000 / speed.coerceIn(.25f, 3f),
                ).toLong().coerceAtLeast(1)
                assertEquals(expected, applied.durationMicros)
            }
        }
    }
}
