// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.artwork

import com.donnclab.kinetiframe.core.actor.artwork.ActorArtworkKey
import com.donnclab.kinetiframe.core.actor.artwork.ArtworkVariant
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class ArtworkTimelineTest {
    private val base = TextureBinding("base", 1f, 2f)
    private val smile = TextureBinding("smile", 3f, 4f)
    private val graph = SceneGraph(
        listOf("mouth"),
        mapOf(
            "mouth" to BoneNode("mouth", name = "Mouth", length = 10f).withArtwork(
                textureBinding = base,
                artworkVariants = mapOf("smile" to ArtworkVariant("Smile", smile)),
            ),
        ),
    )

    @Test fun `partial keys hold inclusive boundaries and preserve authored order`() {
        val keys = listOf(
            ActorArtworkKey(20, linkedMapOf("mouth" to "smile", "eyes" to "open")),
            ActorArtworkKey(10, mapOf("eyes" to "blink")),
            ActorArtworkKey(20, mapOf("hand" to "point")),
        )
        assertEquals(emptyMap<String, String>(), ArtworkTimeline.selections(keys, Long.MIN_VALUE))
        assertEquals(mapOf("eyes" to "blink"), ArtworkTimeline.selections(keys, 19))
        assertEquals(
            linkedMapOf("mouth" to "smile", "eyes" to "blink", "hand" to "point"),
            ArtworkTimeline.selections(keys, 20),
        )
        assertEquals(listOf("mouth", "eyes", "hand"), ArtworkTimeline.selections(keys, Long.MAX_VALUE).keys.toList())
    }

    @Test fun `reset and unknown values retain supplied binding without changing rest or display`() {
        val keys = listOf(ActorArtworkKey(10, mapOf("mouth" to "smile")), ActorArtworkKey(20, mapOf("mouth" to "")))
        val changed = ArtworkTimeline.apply(graph, keys, 10)
        assertEquals(smile, changed.bones.getValue("mouth").textureBinding)
        assertEquals(base, ArtworkTimeline.apply(graph, keys, 20).bones.getValue("mouth").textureBinding)
        assertEquals(changed, ArtworkTimeline.apply(changed, keys, 20))
        assertEquals(
            graph,
            ArtworkTimeline.apply(
                graph,
                listOf(
                    ActorArtworkKey(
                        0,
                        mapOf(
                            "mouth" to "missing",
                            "ghost" to "smile",
                        ),
                    ),
                ),
                0,
            ),
        )
        assertEquals(graph.bones.getValue("mouth").rest, changed.bones.getValue("mouth").rest)
        assertEquals(graph.bones.getValue("mouth").display, changed.bones.getValue("mouth").display)
        assertEquals(base, graph.bones.getValue("mouth").textureBinding)
    }

    @Test fun `empty selections preserve identity but unresolved nonempty selections copy graph`() {
        assertSame(graph, ArtworkTimeline.apply(graph, emptyList(), 0))
        assertSame(graph, ArtworkTimeline.apply(graph, listOf(ActorArtworkKey(0, emptyMap())), 0))
        assertNotSame(graph, ArtworkTimeline.apply(graph, listOf(ActorArtworkKey(0, mapOf("ghost" to "unknown"))), 0))
    }

    @Test fun `unvalidated empty variant key retains legacy lookup behavior`() {
        val bone = graph.bones.getValue(
            "mouth",
        ).withArtwork(artworkVariants = mapOf("" to ArtworkVariant("Raw", smile)))
        val raw = graph.copy(bones = mapOf("mouth" to bone))
        assertEquals(
            smile,
            ArtworkTimeline.apply(raw, listOf(ActorArtworkKey(0, mapOf("mouth" to ""))), 0)
                .bones.getValue("mouth").textureBinding,
        )
    }

    @Test fun `put merges first same time record removes duplicates and sorts without mutating input`() {
        val keys = listOf(
            ActorArtworkKey(20, mapOf("mouth" to "smile")),
            ActorArtworkKey(10, mapOf("eyes" to "blink")),
            ActorArtworkKey(20, mapOf("discarded" to "value")),
        )
        val edited = ArtworkTimeline.put(keys, 20, "hand", "point")
        assertEquals(listOf(10L, 20L), edited.map { it.timeMicros })
        assertEquals(mapOf("mouth" to "smile", "hand" to "point"), edited.last().selections)
        assertEquals(3, keys.size)
        assertEquals(mapOf("mouth" to ""), ArtworkTimeline.put(emptyList(), -1, "mouth", "").single().selections)
    }
}
