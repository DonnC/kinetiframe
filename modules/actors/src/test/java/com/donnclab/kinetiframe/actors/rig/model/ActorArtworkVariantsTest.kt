package com.donnclab.kinetiframe.actors.rig.model

import com.donnclab.kinetiframe.core.actor.artwork.ArtworkVariant
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.actor.figure.DrawnFigure
import com.donnclab.kinetiframe.core.actor.figure.FigurePart
import com.donnclab.kinetiframe.core.actor.figure.FigureShape
import com.donnclab.kinetiframe.core.actor.rigging.BoneArtwork
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ActorArtworkVariantsTest {
    private val shape = listOf(FigureShape("M 0 0 L 10 0 L 10 10 Z", "skin"))
    private val graph = SceneGraph(
        listOf("arm"),
        mapOf(
            "arm" to BoneNode(hierarchy = BoneHierarchy("arm", "Arm", null), rest = BoneRest(50f)),
            "hand" to BoneNode(hierarchy = BoneHierarchy("hand", "Hand", "arm"), rest = BoneRest(20f)),
            "mouth" to BoneNode(
                hierarchy = BoneHierarchy("mouth", "Mouth", "arm"),
                rest = BoneRest(2f),
                artwork = BoneArtwork(
                    textureBinding = TextureBinding("closed", 0f, 0f),
                    artworkVariants = mapOf("open" to ArtworkVariant("Open", TextureBinding("open", 0f, 0f))),
                ),
            ),
        ),
        figure = DrawnFigure(
            palette = mapOf("ink" to "#000000", "skin" to "#F8BF91"),
            parts = listOf(FigurePart("hand", "hand", mapOf("default" to shape, "point" to shape, "fist" to shape))),
        ),
    )

    @Test fun `drawn part variants can be keyed like cutout variants`() {
        assertTrue(ActorArtwork.hasVariant(graph, "hand", "point"))
        assertTrue(ActorArtwork.hasVariant(graph, "mouth", "open"))
        assertTrue(ActorArtwork.hasVariant(graph, "hand", ""))
        assertFalse(ActorArtwork.hasVariant(graph, "hand", "open"))
        assertFalse(ActorArtwork.hasVariant(graph, "arm", "point"))
    }

    @Test fun `the menu lists the default first, for drawn and cutout parts alike`() {
        assertEquals(
            listOf("default" to "Default", "point" to "Point", "fist" to "Fist"),
            ActorArtwork.choices(graph, "hand", "Default"),
        )
        assertEquals(listOf("" to "Default", "open" to "Open"), ActorArtwork.choices(graph, "mouth", "Default"))
        assertTrue(ActorArtwork.choices(graph, "arm", "Default").isEmpty())
    }
}
