// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.presets

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PresetChannelValidationTest {
    private val channel = PresetRotationChannel("head", points = listOf(RotationPoint(0f, 0f), RotationPoint(1f, 0f)))

    @Test fun `legacy controls retain inclusive limits and reject nonfinite or out of range controls`() {
        assertDoesNotThrow {
            PresetChannelValidation.requireValid(
                listOf(PresetRotationChannel("head", PresetOscillation(360f, -180f, 20f, -360f))),
                1,
                true,
            )
        }
        assertEquals(
            PresetChannelIssue.NONFINITE_CONTROLS,
            failure(PresetRotationChannel("head", PresetOscillation(amplitudeDegrees = Float.NaN)), 1),
        )
        assertEquals(
            PresetChannelIssue.INVALID_OSCILLATION,
            failure(PresetRotationChannel("head", PresetOscillation(cyclesPerLoop = -1f)), 1),
        )
        assertEquals(
            PresetChannelIssue.INVALID_PHASE_OFFSET,
            failure(PresetRotationChannel("head", PresetOscillation(phaseDegrees = 361f)), 1),
        )
        assertEquals(PresetChannelIssue.LEGACY_AUTHORED_FIELDS, failure(channel, 1))
    }

    @Test fun `authored point errors preserve validation order`() {
        assertEquals(PresetChannelIssue.EMPTY_PART_ID, failure(channel.copy(boneId = "")))
        assertEquals(PresetChannelIssue.POINT_LIMIT, failure(channel.copy(points = emptyList())))
        assertEquals(
            PresetChannelIssue.INVALID_ENDPOINTS,
            failure(channel.copy(points = listOf(RotationPoint(.1f, 0f), RotationPoint(1f, 0f)))),
        )
        assertEquals(
            PresetChannelIssue.INVALID_POINT,
            failure(channel.copy(points = listOf(RotationPoint(0f, Float.NaN), RotationPoint(1f, 0f)))),
        )
        assertEquals(
            PresetChannelIssue.UNORDERED_POINTS,
            failure(channel.copy(points = listOf(RotationPoint(0f, 0f), RotationPoint(0f, 0f), RotationPoint(1f, 0f)))),
        )
        assertEquals(
            PresetChannelIssue.LOOP_ENDPOINTS,
            failure(channel.copy(points = listOf(RotationPoint(0f, 0f), RotationPoint(1f, 1f)))),
        )
    }

    @Test fun `compensation references must be unique uncompensated other channels`() {
        assertEquals(
            PresetChannelIssue.DUPLICATE_COMPENSATION,
            failure(channel.copy(counterRotationBones = listOf("head", "head"))),
        )
        assertEquals(
            PresetChannelIssue.INVALID_COMPENSATION,
            failure(channel.copy(counterRotationBones = listOf("head"))),
        )
        val parent = channel.copy(boneId = "parent")
        assertDoesNotThrow {
            PresetChannelValidation.requireValid(
                listOf(parent, channel.copy(counterRotationBones = listOf("parent"))),
                3,
                true,
            )
        }
        assertEquals(
            PresetChannelIssue.INVALID_COMPENSATION,
            failure(channel.copy(counterRotationBones = listOf("missing"))),
        )
    }

    @Test fun `channel validation does not take over recipe level schema or channel count policy`() {
        assertDoesNotThrow { PresetChannelValidation.requireValid(emptyList(), 0, true) }
        assertDoesNotThrow { PresetChannelValidation.requireValid(listOf(PresetRotationChannel("head")), 0, true) }
        assertDoesNotThrow { PresetChannelValidation.requireValid(listOf(channel, channel), 2, false) }
    }

    private fun failure(value: PresetRotationChannel, schema: Int = 2): PresetChannelIssue =
        assertThrows(PresetChannelValidationException::class.java) {
            PresetChannelValidation.requireValid(listOf(value), schema, true)
        }.issue
}
