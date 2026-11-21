// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.presets

import com.donnclab.kinetiframe.core.actor.motion.PresetClockMode
import com.donnclab.kinetiframe.core.actor.motion.PresetTiming
import com.donnclab.kinetiframe.core.actor.presets.LegacyPresetSampling
import com.donnclab.kinetiframe.core.actor.presets.PresetOscillation
import com.donnclab.kinetiframe.core.actor.presets.PresetRotationChannel
import com.donnclab.kinetiframe.core.actor.presets.RotationPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PresetRotationProgramTest {
    private val program =
        PresetRotationProgram(listOf(PresetRotationChannel("head", PresetOscillation(30f, 40f, 1f, 90f))))
    private val repeat = LegacyPresetSampling(1f)
    private val oneShot = repeat.copy(mode = PresetClockMode.ONE_SHOT)

    @Test fun `one shot starts displaced then settles and holds offset`() {
        assertEquals(40f, program.legacy(0, oneShot).getValue("head"))
        assertEquals(30f, program.legacy(1_000_000, oneShot).getValue("head"))
        assertEquals(program.legacy(1_000_000, oneShot), program.legacy(Long.MAX_VALUE, oneShot))
        assertEquals(program.legacy(0, oneShot), program.legacy(Long.MIN_VALUE, oneShot))
    }

    @Test fun `repeat and one shot clamp controls and preserve speed scaling`() {
        listOf(repeat, oneShot).forEach { settings ->
            assertEquals(program.legacy(200_000, settings), program.legacy(100_000, settings.copy(speed = 2f)))
            assertEquals(
                program.legacy(200_000, settings.copy(speed = .25f)),
                program.legacy(200_000, settings.copy(speed = -1f)),
            )
            assertEquals(
                program.legacy(200_000, settings.copy(speed = 3f)),
                program.legacy(200_000, settings.copy(speed = 8f)),
            )
            assertEquals(
                program.legacy(200_000, settings.copy(intensity = 2f)),
                program.legacy(200_000, settings.copy(intensity = 5f)),
            )
        }
    }

    @Test fun `counter rotation uses original evaluated channels not chained compensated values`() {
        val points = listOf(RotationPoint(0f, 0f), RotationPoint(1f, 10f))
        val source = listOf(
            PresetRotationChannel("parent", points = points),
            PresetRotationChannel("child", points = points, counterRotationBones = listOf("parent")),
            PresetRotationChannel("tip", points = points, counterRotationBones = listOf("child")),
        )
        val values = PresetRotationProgram(source).authored(500, PresetTiming(1000, PresetClockMode.ONE_SHOT), 2f)
        assertEquals(linkedMapOf("parent" to 10f, "child" to 0f, "tip" to 0f), values)
        assertEquals(listOf("parent", "child", "tip"), values.keys.toList())
    }

    @Test fun `authored time clamps negatives and loops or holds at end`() {
        val authored = PresetRotationProgram(
            listOf(
                PresetRotationChannel(
                    "head",
                    points = listOf(RotationPoint(0f, 2f), RotationPoint(1f, 8f)),
                ),
            ),
        )
        val timing = PresetTiming(1000, PresetClockMode.ONE_SHOT)
        assertEquals(mapOf("head" to 2f), authored.authored(-500, timing))
        assertEquals(mapOf("head" to 8f), authored.authored(2000, timing))
        assertEquals(mapOf("head" to 2f), authored.authored(2000, timing.copy(mode = PresetClockMode.REPEAT)))
    }

    @Test fun `program snapshots lists without changing source or caching samples`() {
        val points = mutableListOf(RotationPoint(0f, 2f), RotationPoint(1f, 8f))
        val channels = mutableListOf(PresetRotationChannel("head", points = points))
        val authored = PresetRotationProgram(channels)
        points.clear()
        channels.clear()
        assertEquals(mapOf("head" to 8f), authored.authored(1000, PresetTiming(1000, PresetClockMode.ONE_SHOT)))
    }

    @Test fun `raw missing compensation fails and legacy ignores authored fields`() {
        val raw = PresetRotationProgram(
            listOf(
                PresetRotationChannel(
                    "head",
                    counterRotationBones = listOf("missing"),
                    points = listOf(RotationPoint(0f, 0f), RotationPoint(1f, 0f)),
                ),
            ),
        )
        assertThrows(NoSuchElementException::class.java) {
            raw.authored(500, PresetTiming(1000, PresetClockMode.ONE_SHOT))
        }
        assertEquals(mapOf("head" to 0f), raw.legacy(500, repeat))
        assertEquals(emptyMap<String, Float>(), PresetRotationProgram(emptyList()).legacy(0, repeat))
    }
}
