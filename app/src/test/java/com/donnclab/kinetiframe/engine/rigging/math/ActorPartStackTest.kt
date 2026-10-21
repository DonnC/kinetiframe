package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.engine.rigging.model.BoneNode
import com.donnclab.kinetiframe.engine.rigging.model.SceneGraph
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class ActorPartStackTest {
    private fun graph() = SceneGraph(
        listOf("torso"),
        linkedMapOf(
            "hand" to BoneNode("hand", parentId = "arm", name = "Hand", length = 10f),
            "torso" to BoneNode("torso", name = "Torso", length = 30f),
            "arm" to BoneNode("arm", parentId = "torso", name = "Arm", length = 20f),
        ),
    )

    @Test fun `legacy ranks preserve skeleton traversal not insertion order`() {
        assertEquals(listOf("torso", "arm", "hand"), ActorPartStack.orderedIds(graph()))
    }

    @Test fun `move changes stacking without reparenting or moving bones`() {
        val original = graph()
        val moved = ActorPartStack.move(original, "hand", false)
        assertEquals(listOf("torso", "hand", "arm"), ActorPartStack.orderedIds(moved))
        original.bones.forEach { (id, bone) ->
            assertEquals(bone, moved.bones.getValue(id).withDisplay(drawOrder = bone.drawOrder))
        }
        assertEquals(original.rootBoneIds, moved.rootBoneIds)
    }

    @Test fun `missing and boundary moves are no ops`() {
        val original = graph()
        assertSame(original, ActorPartStack.move(original, "missing", true))
        assertSame(original, ActorPartStack.move(original, "torso", false))
        assertSame(original, ActorPartStack.move(original, "hand", true))
    }

    @Test fun `cycles and unreachable nodes do not create phantom layers`() {
        val original = graph()
        val graph = original.copy(
            rootBoneIds = listOf("arm"),
            bones = original.bones +
                ("arm" to original.bones.getValue("arm").withHierarchy(parentId = "hand")),
        )
        assertEquals(listOf("arm", "hand"), ActorPartStack.orderedIds(graph))
    }
}
