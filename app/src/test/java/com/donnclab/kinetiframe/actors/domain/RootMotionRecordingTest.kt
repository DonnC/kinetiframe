package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.data.CurveInterpolation
import com.donnclab.kinetiframe.actors.data.PerformanceDocument
import com.donnclab.kinetiframe.actors.data.PerformanceLimits
import com.donnclab.kinetiframe.actors.data.PerformancePlayback
import com.donnclab.kinetiframe.actors.data.RootMotionCurve
import com.donnclab.kinetiframe.actors.data.RootMotionPoint
import com.donnclab.kinetiframe.actors.data.validate
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplates
import com.donnclab.kinetiframe.project.model.StageDimensions
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RootMotionRecordingTest {
    @Test fun `whole character translation carries disconnected roots without stretching children`() {
        val id = rest.bones.keys.first { it !in rest.rootBoneIds }
        val additional = rest.bones.getValue(id).withHierarchy(parentId = null)
        val graph = rest.copy(rootBoneIds = rest.rootBoneIds + id, bones = rest.bones + (id to additional))
        val source = graph.bones.getValue(root).localTransform
        val moved = com.donnclab.kinetiframe.actors.rig.math.ActorPoseEditor.moveRoot(
            graph,
            root,
            androidx.compose.ui.geometry.Offset(source.x + 80, source.y - 12),
            allRoots = true,
        )
        assertEquals(additional.localTransform.x + 80, moved.bones.getValue(id).localTransform.x, .001f)
        assertEquals(additional.localTransform.y - 12, moved.bones.getValue(id).localTransform.y, .001f)
        graph.bones.forEach { (boneId, bone) -> assertEquals(bone.length, moved.bones.getValue(boneId).length) }
    }
    private val rest = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd)
    private val root = rest.rootBoneIds.first()
    private fun document() = PerformanceDocument(
        performanceId = "take", projectId = "scene", actorId = "actor",
        actorRevisionId = "revision", displayName = "Travel", durationMicros = 70_000_000,
        playback = PerformancePlayback.END_HOLD, createdAtEpochMs = 1, updatedAtEpochMs = 1,
    )

    @Test fun `root movement roundtrips and saved evaluator displaces without changing bone lengths`() {
        val source = document().copy(
            rootMotionCurves = listOf(
                RootMotionCurve(
                    root,
                    listOf(RootMotionPoint(0, 0f, 0f), RootMotionPoint(70_000_000, 500f, -20f)),
                    CurveInterpolation.LINEAR,
                ),
            ),
        ).validate()
        val decoded = Json.decodeFromString<PerformanceDocument>(Json.encodeToString(source)).validate()
        assertEquals(source, decoded)
        val posed = PerformanceEvaluator.pose(decoded, 35_000_000, rest)
        assertEquals(
            rest.bones.getValue(root).localTransform.x + 250f,
            posed.bones.getValue(root).localTransform.x,
            .001f,
        )
        rest.bones.forEach { (id, bone) -> assertEquals(bone.length, posed.bones.getValue(id).length) }
    }

    @Test fun `older schema defaults root curves and duration has independent bounds`() {
        val encoded = Json.encodeToString(document().copy(schemaVersion = 6, durationMicros = 2_000_000))
        assertTrue(Json.decodeFromString<PerformanceDocument>(encoded).validate().rootMotionCurves.isEmpty())
        document().copy(durationMicros = PerformanceLimits.MAX_DURATION_MICROS).validate()
        assertThrows(IllegalArgumentException::class.java) {
            document().copy(
                durationMicros =
                PerformanceLimits.MAX_DURATION_MICROS + 1,
            ).validate()
        }
    }

    @Test fun `root filtering is not angular and preserves other roots and boundary samples`() {
        val untouched = RootMotionCurve("other", listOf(RootMotionPoint(0, 2f, 3f)))
        val samples =
            listOf(RootMotionPoint(0, 0f, 0f), RootMotionPoint(100_000, 400f, 0f), RootMotionPoint(200_000, 800f, 0f))
        val result = RecordedRootMotionCurves.merge(
            listOf(untouched),
            mapOf(root to samples),
            0,
            200_000,
            200_000,
            .35f,
        )
        assertSame(untouched, result.first())
        assertEquals(800f, result.last().points.last().x)
        assertThrows(IllegalArgumentException::class.java) {
            RootMotionCurve(root, listOf(RootMotionPoint(0, Float.NaN, 0f))).validate(1)
        }
    }
}
