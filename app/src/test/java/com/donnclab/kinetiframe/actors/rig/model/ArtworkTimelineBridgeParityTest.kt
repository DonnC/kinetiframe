package com.donnclab.kinetiframe.actors.rig.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArtworkTimelineBridgeParityTest {
    private val base = TextureBinding("base", 0f, 0f)
    private val alternative = TextureBinding("alternative", 2f, 3f)
    private val graph = SceneGraph(
        listOf("part"),
        mapOf(
            "part" to BoneNode("part", name = "Part", length = 10f).withArtwork(
                textureBinding = base,
                artworkVariants = mapOf("alternate" to ArtworkVariant("Alternate", alternative)),
            ),
        ),
    )

    @Test fun `bridge matches former authored order evaluation and graph switching`() {
        val records = listOf(
            ActorArtworkKey(20, mapOf("part" to "alternate")),
            ActorArtworkKey(10, mapOf("ghost" to "unknown")),
            ActorArtworkKey(30, mapOf("part" to "")),
            ActorArtworkKey(20, mapOf("part" to "missing")),
        )
        val sources = listOf(emptyList(), records, records.reversed(), records.take(3))
        sources.forEach { keys ->
            listOf(Long.MIN_VALUE, -1L, 0L, 9L, 10L, 19L, 20L, 29L, 30L, Long.MAX_VALUE).forEach { time ->
                val selected = legacySelections(keys, time)
                assertEquals(selected, ActorArtwork.selections(keys, time))
                val expected = if (selected.isEmpty()) {
                    graph
                } else {
                    graph.copy(
                        bones = graph.bones.mapValues { (id, bone) ->
                            val variant = bone.artworkVariants[selected[id]]
                            if (variant == null) bone else bone.withArtwork(textureBinding = variant.binding)
                        },
                    )
                }
                assertEquals(expected, ActorArtwork.apply(graph, keys, time))
            }
        }
    }

    @Test fun `bridge matches former same time editing including duplicate input`() {
        val keys = listOf(
            ActorArtworkKey(20, mapOf("part" to "alternate")),
            ActorArtworkKey(10, mapOf("ghost" to "unknown")),
            ActorArtworkKey(20, mapOf("lost" to "value")),
        )
        listOf(-1L, 10L, 20L, Long.MAX_VALUE).forEach { time ->
            val existing = keys.firstOrNull { it.timeMicros == time }?.selections.orEmpty()
            val expected = (
                keys.filterNot { it.timeMicros == time } +
                    ActorArtworkKey(time, existing + ("part" to ""))
                ).sortedBy { it.timeMicros }
            assertEquals(expected, ActorArtwork.put(keys, time, "part", ""))
        }
    }

    private fun legacySelections(keys: List<ActorArtworkKey>, time: Long): Map<String, String> = buildMap {
        keys.forEach { if (it.timeMicros <= time) putAll(it.selections) }
    }
}
