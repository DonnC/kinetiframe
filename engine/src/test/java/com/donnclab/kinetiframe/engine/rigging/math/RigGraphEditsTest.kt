// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.actor.rigging.Transform
import com.donnclab.kinetiframe.core.geometry.Point2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class RigGraphEditsTest {
    @Test fun `rotation only changes the targeted rest angle and shares unrelated records`() {
        val target = BoneNode("target", name = "Target", length = 10f)
            .withRest(localTransform = Transform(Point2(2f, 3f), 10f, Point2(-2f, 3f)))
            .withArtwork(textureBinding = TextureBinding("body", 2f, 3f)).withDisplay(visible = false)
        val other = BoneNode("other", name = "Other", length = 1f)
        val graph = SceneGraph(listOf("target", "other"), mapOf("target" to target, "other" to other))
        val updated = RigGraphEdits.rotate(graph, "target", 45f)
        val bone = updated.bones.getValue("target")
        assertNotSame(graph, updated)
        assertSame(other, updated.bones.getValue("other"))
        assertSame(target.hierarchy, bone.hierarchy)
        assertSame(target.artwork, bone.artwork)
        assertSame(target.display, bone.display)
        assertEquals(target.localTransform.translation, bone.localTransform.translation)
        assertEquals(target.localTransform.stretch, bone.localTransform.stretch)
        assertEquals(45f, bone.localTransform.rotationDegrees)
        assertEquals(10f, target.localTransform.rotationDegrees)
    }

    @Test fun `unknown target retains graph identity and raw values still await validation`() {
        val target = BoneNode("target", name = "Target", length = 10f)
        val graph = SceneGraph(listOf("target"), mapOf("target" to target))
        assertSame(graph, RigGraphEdits.rotate(graph, "missing", 45f))
        assertEquals(
            Float.POSITIVE_INFINITY,
            RigGraphEdits.rotate(
                graph,
                "target",
                Float.POSITIVE_INFINITY,
            ).bones.getValue("target").localTransform.rotationDegrees,
        )
    }
}
