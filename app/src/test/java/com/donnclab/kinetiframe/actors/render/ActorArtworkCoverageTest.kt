package com.donnclab.kinetiframe.actors.render

import com.donnclab.kinetiframe.actors.rig.model.BoneNode
import com.donnclab.kinetiframe.actors.rig.model.SceneGraph
import com.donnclab.kinetiframe.actors.rig.model.TextureBinding
import com.donnclab.kinetiframe.core.actor.rigging.BoneArtwork
import com.donnclab.kinetiframe.core.actor.rigging.BoneDisplay
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ActorArtworkCoverageTest {
    @Test fun `partially decoded actor reports the exact missing bound part`() {
        val graph = SceneGraph(
            bones = listOf("head", "arm").associateWith { id ->
                BoneNode(
                    hierarchy = BoneHierarchy(id, id, null),
                    rest = BoneRest(10f),
                    artwork = BoneArtwork(
                        textureBinding = TextureBinding(assetId = "art-$id", pivotX = 0f, pivotY = 0f),
                    ),
                )
            },
        )
        assertEquals(listOf("arm"), ActorArtworkCoverage.missingParts(graph, setOf("art-head")))
        assertTrue(ActorArtworkCoverage.missingParts(graph, setOf("art-head", "art-arm")).isEmpty())
    }

    @Test fun `unbound and intentionally hidden parts do not require artwork`() {
        val graph = SceneGraph(
            bones = mapOf(
                "guide" to BoneNode(id = "guide", name = "Guide", length = 10f),
                "hidden" to BoneNode(
                    hierarchy = BoneHierarchy("hidden", "Hidden", null),
                    rest = BoneRest(10f),
                    artwork = BoneArtwork(
                        textureBinding = TextureBinding(assetId = "hidden-art", pivotX = 0f, pivotY = 0f),
                    ),
                    display = BoneDisplay(visible = false),
                ),
            ),
        )
        assertTrue(ActorArtworkCoverage.missingParts(graph, emptySet()).isEmpty())
    }
}
