package com.donnclab.kinetiframe.actors.rig

import com.donnclab.kinetiframe.actors.rig.math.ForwardKinematicsSolver
import com.donnclab.kinetiframe.actors.rig.model.updateBoneRotation
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.actor.rigging.Transform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class ForwardKinematicsSolverTest {
    @Test
    fun `resolves parent rotation and child translation`() {
        val root = BoneNode(
            hierarchy = BoneHierarchy("root", "A", null),
            rest = BoneRest(50f, localTransform = Transform(100f, 100f)),
        )
        val child = BoneNode(
            hierarchy = BoneHierarchy("child", "B", "root"),
            rest = BoneRest(50f, localTransform = Transform(x = 50f, rotationDegrees = 90f)),
        )
        val result = ForwardKinematicsSolver.resolveWorldStates(
            SceneGraph(rootBoneIds = listOf("root"), bones = mapOf(root.id to root, child.id to child)),
        )

        assertEquals(150f, result.getValue("root").endPoint.x, 0.001f)
        assertEquals(100f, result.getValue("root").endPoint.y, 0.001f)
        assertEquals(150f, result.getValue("child").startPoint.x, 0.001f)
        assertEquals(100f, result.getValue("child").startPoint.y, 0.001f)
        assertEquals(150f, result.getValue("child").endPoint.x, 0.001f)
        assertEquals(150f, result.getValue("child").endPoint.y, 0.001f)
    }

    @Test
    fun `rotation update preserves untouched node identity`() {
        val untouched = BoneNode("untouched", name = "Untouched", length = 10f)
        val target = BoneNode("target", name = "Target", length = 10f)
        val old = SceneGraph(listOf("untouched", "target"), mapOf(untouched.id to untouched, target.id to target))
        val updated = updateBoneRotation(old, "target", 45f)

        assertNotSame(old, updated)
        assertSame(untouched, updated.bones.getValue("untouched"))
        assertEquals(45f, updated.bones.getValue("target").localTransform.rotationDegrees, 0.001f)
    }
}
