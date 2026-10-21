package com.donnclab.kinetiframe.engine.rigging.presets

import com.donnclab.kinetiframe.core.actor.presets.PresetOscillation
import com.donnclab.kinetiframe.core.actor.presets.PresetRotationChannel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalSerializationApi::class)
class PresetChannelCodecBridgeTest {
    private val formats = listOf(Json, Json { encodeDefaults = true })

    @Test fun `neutral codec matches former generated channel codec including signed zero`() {
        val fixtures = listOf(
            RotationChannel("head"),
            RotationChannel("head", -0f, -0f, 1f, -0f),
            RotationChannel("head", 10f, -20f, .5f, 90f),
            RotationChannel(
                "head",
                points = listOf(RotationPoint(0f, 0f), RotationPoint(1f, 10f)),
                counterRotationBones = listOf("torso"),
            ),
        )
        fixtures.forEach { legacy ->
            val neutral = PresetRotationChannel(
                legacy.boneId,
                PresetOscillation(
                    legacy.offsetDegrees,
                    legacy.amplitudeDegrees,
                    legacy.cyclesPerLoop,
                    legacy.phaseDegrees,
                ),
                legacy.points,
                legacy.counterRotationBones,
            )
            formats.forEach { json ->
                assertEquals(json.encodeToString(legacy), json.encodeToString(neutral))
                assertEquals(
                    json.encodeToString(legacy),
                    json.encodeToString(json.decodeFromString<PresetRotationChannel>(json.encodeToString(legacy))),
                )
            }
        }
        assertEquals(
            RotationChannel.serializer().descriptor.serialName,
            PresetRotationChannel.serializer().descriptor.serialName,
        )
    }
}
