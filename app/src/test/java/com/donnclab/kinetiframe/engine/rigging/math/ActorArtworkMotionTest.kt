package com.donnclab.kinetiframe.engine.rigging.math

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.core.actor.rigging.BoneArtwork
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.data.actor.ActorRevision
import com.donnclab.kinetiframe.data.actor.PerformanceDocument
import com.donnclab.kinetiframe.data.actor.PerformanceLimits
import com.donnclab.kinetiframe.data.actor.PerformancePlayback
import com.donnclab.kinetiframe.data.actor.validate
import com.donnclab.kinetiframe.domain.actor.PerformanceEvaluator
import com.donnclab.kinetiframe.engine.rigging.model.ActorArtwork
import com.donnclab.kinetiframe.engine.rigging.model.ActorArtworkKey
import com.donnclab.kinetiframe.engine.rigging.model.ArtworkVariant
import com.donnclab.kinetiframe.engine.rigging.model.BoneNode
import com.donnclab.kinetiframe.engine.rigging.model.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.model.TextureBinding
import com.donnclab.kinetiframe.engine.rigging.model.Transform
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Contract: expression timing cannot change skeletal motion; endpoint posing cannot stretch limbs. */
class ActorArtworkMotionTest {
    @Test fun `expression loop duration preserves whole gait cycles within storage limit`() {
        val preset = com.donnclab.kinetiframe.engine.rigging.presets.AppliedActorPreset(
            com.donnclab.kinetiframe.engine.rigging.presets.ActorMotionPreset(
                id = "walk",
                name = "Walk",
                description = "",
                durationSeconds = 1.8f,
                requiredBones = emptyList(),
                channels = emptyList(),
            ),
        )
        val duration = com.donnclab.kinetiframe.domain.actor.PerformanceDuration.expressionLoop(2_000_000, preset)
        assertEquals(preset.durationMicros * 2, duration)
        val limit = PerformanceLimits.MAX_DURATION_MICROS
        val bounded = com.donnclab.kinetiframe.domain.actor.PerformanceDuration.expressionLoop(limit, preset)
        assertTrue(bounded <= limit)
        assertEquals(0L, bounded % preset.durationMicros)
    }
    private val base = TextureBinding("mouth-default", 2f, 3f)
    private val smile = TextureBinding("mouth-smile", 4f, 5f).let {
        it.copy(alignment = it.alignment.copy(scale = .5f))
    }
    private val graph =
        SceneGraph(
            listOf("mouth"),
            mapOf(
                "mouth" to BoneNode(
                    hierarchy = BoneHierarchy("mouth", "Mouth", null),
                    rest = BoneRest(10f),
                    artwork = BoneArtwork(
                        textureBinding = base,
                        artworkVariants = mapOf("smile" to ArtworkVariant("Smile", smile)),
                    ),
                ),
            ),
        )

    @Test fun `variants hold until reset without touching original rig`() {
        val keys = listOf(ActorArtworkKey(100, mapOf("mouth" to "smile")), ActorArtworkKey(300, mapOf("mouth" to "")))
        assertEquals(base, ActorArtwork.apply(graph, keys, 99).bones.getValue("mouth").textureBinding)
        assertEquals(smile, ActorArtwork.apply(graph, keys, 299).bones.getValue("mouth").textureBinding)
        assertEquals(base, ActorArtwork.apply(graph, keys, 300).bones.getValue("mouth").textureBinding)
        assertEquals(base, graph.bones.getValue("mouth").textureBinding)
        assertEquals(setOf("mouth", "mouth#smile"), ActorArtwork.bindings(graph).keys)
    }

    @Test fun `partial expression keys accumulate and same-time edits merge`() {
        var keys = ActorArtwork.put(emptyList(), 200, "mouth", "smile")
        keys = ActorArtwork.put(keys, 100, "eyes", "blink")
        keys = ActorArtwork.put(keys, 200, "hand", "point")
        assertEquals(listOf(100L, 200L), keys.map { it.timeMicros })
        assertEquals(
            mapOf("eyes" to "blink", "mouth" to "smile", "hand" to "point"),
            ActorArtwork.selections(keys, 250),
        )
    }

    @Test fun `saved performance and actor preserve variants and timed swaps`() {
        val revision = ActorRevision(
            revisionId = "revision",
            actorId = "actor",
            sceneGraph = graph,
            artworkAssetPaths = mapOf("mouth" to "base.png", "mouth#smile" to "smile.png"),
            createdAtEpochMs = 1,
        ).validate()
        val restored = Json.decodeFromString<ActorRevision>(Json.encodeToString(revision)).validate()
        val performance = PerformanceDocument(
            performanceId = "performance", projectId = "project", actorId = "actor",
            actorRevisionId = "revision",
            displayName = "Talk",
            durationMicros = 1000,
            playback = PerformancePlayback.END_HOLD,
            artworkKeys = listOf(
                ActorArtworkKey(100, mapOf("mouth" to "smile")),
            ),
            createdAtEpochMs = 1, updatedAtEpochMs = 1,
        ).validate()
        val loaded = Json.decodeFromString<PerformanceDocument>(Json.encodeToString(performance)).validate()
        assertEquals(
            smile,
            PerformanceEvaluator.pose(loaded, 500, restored.sceneGraph).bones.getValue("mouth").textureBinding,
        )
        assertEquals(
            base,
            PerformanceEvaluator.pose(
                loaded.copy(playback = PerformancePlayback.LOOP),
                1050,
                graph,
            ).bones.getValue("mouth").textureBinding,
        )
        assertThrows(IllegalArgumentException::class.java) {
            performance.copy(artworkKeys = listOf(ActorArtworkKey(1001, emptyMap()))).validate()
        }
    }

    private fun limb() = SceneGraph(
        listOf("upper"),
        mapOf(
            "upper" to BoneNode("upper", name = "Upper", length = 100f),
            "lower" to BoneNode(
                hierarchy = BoneHierarchy("lower", "Lower", "upper"),
                rest = BoneRest(
                    100f,
                    localTransform = Transform(x = 100f, rotationDegrees = 90f),
                    ikChainRootId = "upper",
                ),
            ),
            "hand" to BoneNode(
                hierarchy = BoneHierarchy("hand", "Hand", "lower"),
                rest = BoneRest(20f, localTransform = Transform(x = 100f, rotationDegrees = -90f)),
            ),
        ),
    )

    @Test fun `hand drag moves whole arm while retaining wrist orientation and lengths`() {
        val original = limb()
        val target = Offset(120f, 120f)
        val posed = ActorPoseEditor.pose(original, "hand", target)
        val world = RigGeometry.resolve(posed)
        assertEquals(0f, (world.getValue("hand").end - target).getDistance(), .01f)
        assertEquals(0f, world.getValue("hand").angle, .01f)
        original.bones.forEach { (id, bone) -> assertEquals(bone.length, posed.bones.getValue(id).length) }
        assertNotEquals(
            original.bones.getValue("upper").localTransform.rotationDegrees,
            posed.bones.getValue("upper").localTransform.rotationDegrees,
        )
    }

    @Test fun `slow endpoint drag has no bend flip or discontinuous hand rotation`() {
        val original = limb()
        var previous = RigGeometry.resolve(original).getValue("lower").angle
        for (step in 0..100) {
            val posed = ActorPoseEditor.pose(original, "hand", Offset(120f + step * .3f, 120f - step * .2f))
            val world = RigGeometry.resolve(posed)
            assertTrue(kotlin.math.abs(world.getValue("lower").angle - previous) < 30f)
            assertEquals(0f, world.getValue("hand").angle, .01f)
            previous = world.getValue("lower").angle
        }
    }
}
