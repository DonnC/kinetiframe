package com.donnclab.kinetiframe.actors.rig.math

import com.donnclab.kinetiframe.core.actor.figure.DrawnFigure
import com.donnclab.kinetiframe.core.actor.figure.FigureTube
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PoseHandlesTest {
    private fun graph(vararg bones: Triple<String, String?, Float>) = SceneGraph(
        bones.filter { it.second == null }.map { it.first },
        bones.associate { (id, parent, length) ->
            id to BoneNode(hierarchy = BoneHierarchy(id, id, parent), rest = BoneRest(length))
        },
    )

    @Test fun `a person gets handles at hands, feet and head, not at every joint`() {
        val person = graph(
            Triple("torso", null, 100f),
            Triple("head", "torso", 80f),
            Triple("eyes", "head", 1f),
            Triple("mouth", "head", 1f),
            Triple("upper", "torso", 50f),
            Triple("lower", "upper", 50f),
            Triple("hand", "lower", 20f),
            Triple("thigh", "torso", 60f),
            Triple("shin", "thigh", 60f),
            Triple("foot", "shin", 20f),
        )
        assertEquals(setOf("head", "hand", "foot"), PoseHandles.ids(person))
    }

    @Test fun `any other thing gets handles at its chain tips`() {
        val dragon = graph(
            Triple("body", null, 100f),
            Triple("tail1", "body", 40f),
            Triple("tail2", "tail1", 40f),
            Triple("wing", "body", 70f),
            Triple("wing_tip", "wing", 50f),
        )
        assertEquals(setOf("tail2", "wing_tip"), PoseHandles.ids(dragon))
        // A single bone is its own tip, such as a pendulum or a flagpole.
        assertEquals(setOf("pole"), PoseHandles.ids(graph(Triple("pole", null, 100f))))
    }

    @Test fun `figure pieces count as covering their bones`() {
        val arm = graph(Triple("upper", null, 50f), Triple("lower", "upper", 50f), Triple("spare", "upper", 10f))
        val drawn = arm.copy(
            figure = DrawnFigure(
                palette = mapOf("ink" to "#000000", "shirt" to "#12B1C2"),
                tubes = listOf(FigureTube("arm", listOf("upper", "lower"), 40f, "shirt")),
            ),
        )
        assertTrue(PoseHandles.isCovered(drawn, "upper"))
        assertTrue(PoseHandles.isCovered(drawn, "lower"))
        assertFalse(PoseHandles.isCovered(drawn, "spare"))
        assertFalse(PoseHandles.isCovered(arm, "upper"))
    }
}
