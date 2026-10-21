package com.donnclab.kinetiframe.engine.rigging.presets

import com.donnclab.kinetiframe.core.actor.presets.AppliedPresetDocument
import com.donnclab.kinetiframe.engine.rigging.model.BoneNode
import com.donnclab.kinetiframe.engine.rigging.model.SceneGraph
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class AppliedPresetProgramBridgeTest {
    private val graph = SceneGraph(listOf("root"), mapOf("root" to BoneNode("root", name = "Root", length = 120f)))

    @Test fun `shared program matches original product orchestration across shipped recipes`() {
        val directory = File("src/main/assets/actor/presets").takeIf { it.isDirectory }
            ?: File("app/src/main/assets/actor/presets")
        val recipes = directory.walkTopDown().filter { it.isFile && it.extension == "json" }
            .map { Json.decodeFromString<ActorMotionPreset>(it.readText()).validate() }.toList()
        assertTrue(recipes.isNotEmpty())
        val roots =
            listOf(
                RootMotionChannel(
                    "root",
                    listOf(RootMotionPoint(0f), RootMotionPoint(.5f, .3f, -.2f), RootMotionPoint(1f)),
                ),
            )
        val sampler = PresetRootMotionSampler(1)
        recipes.forEach { recipe ->
            for (schema in listOf(1, 2, 3)) {
                for (loop in listOf(false, true)) {
                    for (speed in listOf(.1f, 1.3f, 5f)) {
                        for (intensity in listOf(-1f, .7f, 3f)) {
                            val applied =
                                AppliedActorPreset(
                                    recipe.copy(schemaVersion = schema, loop = loop, rootMotion = roots),
                                    intensity,
                                    speed,
                                )
                            val program =
                                AppliedPresetProgram(AppliedPresetDocument(applied.recipe.document(), intensity, speed))
                            assertEquals(applied.durationMicros, program.durationMicros)
                            listOf(
                                Long.MIN_VALUE,
                                -1L,
                                0L,
                                1L,
                                123_456L,
                                applied.durationMicros,
                                Long.MAX_VALUE,
                            ).forEach { time ->
                                val actual = program.offsets(time)
                                assertEquals(applied.offsets(time), actual)
                                assertEquals(LegacyPresetRotationReference.offsets(applied, time), actual)
                                assertEquals(applied.offsets(time).keys.toList(), actual.keys.toList())
                                assertEquals(
                                    applied.rootTranslations(time, graph),
                                    program.rootTranslations(time, graph, sampler),
                                )
                                assertEquals(
                                    LegacyPresetSamplingReference.roots(applied, time, graph),
                                    program.rootTranslations(time, graph, sampler),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
