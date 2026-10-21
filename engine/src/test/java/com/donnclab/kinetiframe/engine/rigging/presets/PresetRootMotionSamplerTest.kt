// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.presets

import com.donnclab.kinetiframe.core.actor.motion.RootTranslation
import com.donnclab.kinetiframe.core.actor.presets.PresetRootSampling
import com.donnclab.kinetiframe.core.actor.presets.RootMotionChannel
import com.donnclab.kinetiframe.core.actor.presets.RootMotionPoint
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.motion.CompilationCacheException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PresetRootMotionSamplerTest {
    private val graph = SceneGraph(
        listOf("root"),
        mapOf(
            "root" to BoneNode("root", name = "Root", length = 100f),
            "child" to BoneNode("child", "root", "Child", 50f),
        ),
    )
    private val channel = RootMotionChannel(
        "root",
        listOf(
            RootMotionPoint(0f),
            RootMotionPoint(.5f, .5f, -.25f),
            RootMotionPoint(1f),
        ),
    )
    private val sampler = PresetRootMotionSampler()

    @Test fun `root travel scales with proportions and clamped intensity`() {
        assertEquals(RootTranslation(50f, -25f), sample(500, PresetRootSampling(1000, true)))
        assertEquals(RootTranslation(100f, -50f), sample(500, PresetRootSampling(1000, true, 8f)))
        assertEquals(RootTranslation(0f, -0f), sample(500, PresetRootSampling(1000, true, -1f)))
        val doubled = graph.copy(bones = graph.bones + ("root" to graph.bones.getValue("root").withRest(length = 200f)))
        assertEquals(
            RootTranslation(100f, -50f),
            sampler.evaluate(
                listOf(channel),
                500,
                doubled,
                PresetRootSampling(1000, true),
            ).getValue("root"),
        )
    }

    @Test fun `loop floor modulus differs from clamped one shot`() {
        assertEquals(sample(500, PresetRootSampling(1000, true)), sample(-500, PresetRootSampling(1000, true)))
        assertEquals(RootTranslation(0f, 0f), sample(-500, PresetRootSampling(1000, false)))
        assertEquals(sample(0, PresetRootSampling(1000, false)), sample(2000, PresetRootSampling(1000, false)))
    }

    @Test fun `child and unknown channels retain entries with zero scale`() {
        val channels = listOf(channel.copy(boneId = "child"), channel.copy(boneId = "unknown"))
        val result = sampler.evaluate(channels, 500, graph, PresetRootSampling(1000, true))
        assertEquals(listOf("child", "unknown"), result.keys.toList())
        result.values.forEach { assertEquals(RootTranslation(0f, -0f), it) }
    }

    @Test fun `cache eviction clear and loop policy do not change results`() {
        val bounded = PresetRootMotionSampler(1)
        val settings = PresetRootSampling(1000, true)
        val expected = bounded.evaluate(listOf(channel), 250, graph, settings)
        bounded.evaluate(
            listOf(
                channel.copy(
                    points = channel.points.map {
                        it.copy(x = it.x + .1f)
                    },
                ),
            ),
            250,
            graph,
            settings,
        )
        bounded.evaluate(listOf(channel), 250, graph, settings.copy(loop = false))
        assertEquals(
            expected,
            bounded.evaluate(listOf(channel.copy(points = channel.points.toList())), 250, graph, settings),
        )
        bounded.clear()
        assertEquals(expected, bounded.evaluate(listOf(channel), 250, graph, settings))
        assertThrows(CompilationCacheException::class.java) { PresetRootMotionSampler(0) }
    }

    @Test fun `empty channels return before invalid duration is consumed`() {
        assertEquals(
            emptyMap<String, RootTranslation>(),
            sampler.evaluate(emptyList(), 0, graph, PresetRootSampling(0, true)),
        )
        assertThrows(ArithmeticException::class.java) { sample(0, PresetRootSampling(0, true)) }
    }

    private fun sample(time: Long, sampling: PresetRootSampling): RootTranslation =
        sampler.evaluate(listOf(channel), time, graph, sampling).getValue("root")
}
