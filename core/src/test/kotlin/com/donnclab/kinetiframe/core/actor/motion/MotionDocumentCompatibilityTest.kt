// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.motion

import com.donnclab.kinetiframe.core.document.StageDimensionException
import com.donnclab.kinetiframe.core.document.StageDimensionIssue
import com.donnclab.kinetiframe.core.document.StageDimensions
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

@OptIn(ExperimentalSerializationApi::class)
class MotionDocumentCompatibilityTest {
    private val json = Json { encodeDefaults = true }

    @Test fun `legacy rotation channel keeps wire fields defaults and descriptor`() {
        val source = """{"boneId":"arm","interpolation":"LINEAR","points":[{"timeMicros":0,"degrees":15.0}]}"""
        val decoded = json.decodeFromString<RotationCurve>(source).requireValid(100)
        assertEquals(json.parseToJsonElement(source), json.parseToJsonElement(json.encodeToString(decoded)))
        assertEquals("com.kinetiframe.data.actor.RotationCurve", RotationCurve.serializer().descriptor.serialName)
        val missingMode = Json.decodeFromString<RotationCurve>(
            """{"boneId":"arm","points":[{"timeMicros":0,"degrees":15.0}]}""",
        )
        assertEquals(CurveInterpolation.LINEAR, missingMode.interpolation)
    }

    @Test fun `legacy root channel and translation keep shape and defaults`() {
        val source = """{"rootId":"body","points":[{"timeMicros":0,"x":2.0,"y":-3.0}],"interpolation":"SMOOTH"}"""
        val decoded = json.decodeFromString<RootMotionCurve>(source).requireValid(100)
        assertEquals(json.parseToJsonElement(source), json.parseToJsonElement(json.encodeToString(decoded)))
        assertEquals("com.kinetiframe.data.actor.RootMotionCurve", RootMotionCurve.serializer().descriptor.serialName)
        assertEquals(RootTranslation(), Json.decodeFromString<RootTranslation>("{}"))
        assertEquals(
            "com.kinetiframe.engine.rigging.model.RootTranslation",
            RootTranslation.serializer().descriptor.serialName,
        )
    }

    @Test fun `enum wire names and serial descriptors remain stable`() {
        assertEquals(
            "com.kinetiframe.data.actor.CurveInterpolation",
            CurveInterpolation.serializer().descriptor.serialName,
        )
        assertEquals(
            "com.kinetiframe.data.actor.PerformancePlayback",
            PerformancePlayback.serializer().descriptor.serialName,
        )
        CurveInterpolation.entries.forEach {
            assertEquals(it, Json.decodeFromString<CurveInterpolation>("\"${it.name}\""))
        }
        PerformancePlayback.entries.forEach {
            assertEquals(it, Json.decodeFromString<PerformancePlayback>("\"${it.name}\""))
        }
    }

    @Test fun `invalid motion is rejected without reordering or truncating samples`() {
        val unsorted = listOf(RotationCurvePoint(100, 10f), RotationCurvePoint(0, 20f))
        val curve = RotationCurve("arm", points = unsorted)
        val order = assertThrows(MotionValidationException::class.java) { curve.requireValid(100) }
        assertEquals(MotionValidationIssue.UNORDERED_POINTS, order.issue)
        assertEquals(unsorted, curve.points)
        val budget = assertThrows(MotionValidationException::class.java) {
            RotationCurve(
                "arm",
                points = List(RotationCurve.MAX_POINTS + 1) {
                    RotationCurvePoint(it.toLong(), 0f)
                },
            ).requireValid(1000)
        }
        assertEquals(MotionValidationIssue.POINT_LIMIT, budget.issue)
        val nonFinite = assertThrows(MotionValidationException::class.java) {
            RootMotionCurve("body", listOf(RootMotionPoint(0, Float.NaN, 0f))).requireValid(1)
        }
        assertEquals(MotionValidationIssue.INVALID_ROOT_POINT, nonFinite.issue)
    }

    @Test fun `stage geometry keeps native dimensions descriptor and invalid input guard`() {
        val source = """{"width":1920,"height":1080}"""
        assertEquals(StageDimensions.LandscapeHd, Json.decodeFromString<StageDimensions>(source))
        assertEquals(source, Json.encodeToString(StageDimensions.LandscapeHd))
        assertEquals("com.kinetiframe.engine.model.StageDimensions", StageDimensions.serializer().descriptor.serialName)
        val failure = assertThrows(StageDimensionException::class.java) { StageDimensions(0, 1080) }
        assertEquals(StageDimensionIssue.NON_POSITIVE_WIDTH, failure.issue)
    }
}
