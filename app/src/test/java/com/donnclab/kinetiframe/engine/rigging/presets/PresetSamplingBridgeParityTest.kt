package com.donnclab.kinetiframe.engine.rigging.presets

import com.donnclab.kinetiframe.engine.rigging.model.BoneNode
import com.donnclab.kinetiframe.engine.rigging.model.RootTranslation
import com.donnclab.kinetiframe.engine.rigging.model.SceneGraph
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class PresetSamplingBridgeParityTest {
    private val graph = SceneGraph(listOf("root"), mapOf("root" to BoneNode("root", name = "Root", length = 120f)))

    @Test fun `all shipped authored channels retain exact former interpolation`() {
        val recipes = recipes().filter { it.schemaVersion >= 2 }
        assertTrue(recipes.isNotEmpty())
        recipes.forEach { recipe ->
            recipe.channels.forEach { channel ->
                for (step in -1..101) {
                    val progress = step / 100.0
                    assertEquals(
                        LegacyPresetSamplingReference.curve(channel.points, progress, recipe.loop),
                        PresetCurve.evaluate(channel.points, progress, recipe.loop),
                        "${recipe.id}/${channel.boneId}/$progress",
                    )
                }
            }
        }
    }

    @Test fun `root bridge matches former scaling timing and settings without altering snapshots`() {
        val channel = RootMotionChannel(
            "root",
            listOf(
                RootMotionPoint(0f),
                RootMotionPoint(.3f, .2f, -.1f),
                RootMotionPoint(.7f, -.1f, .3f),
                RootMotionPoint(1f),
            ),
        )
        val prototype = recipes().first().copy(schemaVersion = 3, rootMotion = listOf(channel))
        listOf(false, true).forEach { loop ->
            listOf(.1f, 1f, 5f).forEach { speed ->
                listOf(-1f, .5f, 3f).forEach { intensity ->
                    val applied = AppliedActorPreset(prototype.copy(loop = loop), intensity, speed)
                    val wire = Json.encodeToString(applied)
                    listOf(
                        Long.MIN_VALUE,
                        -1L,
                        0L,
                        1L,
                        applied.durationMicros / 3,
                        applied.durationMicros,
                        applied.durationMicros * 3,
                        Long.MAX_VALUE,
                    ).forEach { time ->
                        assertEquals(
                            LegacyPresetSamplingReference.roots(applied, time, graph),
                            applied.rootTranslations(time, graph),
                        )
                    }
                    val restored = Json.decodeFromString<AppliedActorPreset>(wire)
                    assertEquals(applied.rootTranslations(123_456, graph), restored.rootTranslations(123_456, graph))
                    assertEquals(wire, Json.encodeToString(applied))
                }
            }
        }
    }

    @Test fun `empty root bridge keeps early return for unvalidated settings`() {
        val applied = AppliedActorPreset(recipes().first().copy(rootMotion = emptyList()), speed = Float.NaN)
        assertEquals(emptyMap<String, RootTranslation>(), applied.rootTranslations(0, graph))
    }

    private fun recipes(): List<ActorMotionPreset> {
        val directory = File("src/main/assets/actor/presets").takeIf { it.isDirectory }
            ?: File("app/src/main/assets/actor/presets")
        return directory.walkTopDown().filter { it.isFile && it.extension == "json" }
            .map { Json.decodeFromString<ActorMotionPreset>(it.readText()).validate() }.toList()
    }
}
