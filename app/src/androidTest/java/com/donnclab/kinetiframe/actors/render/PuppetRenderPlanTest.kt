package com.donnclab.kinetiframe.actors.render

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.actor.rigging.BoneArtwork
import com.donnclab.kinetiframe.core.actor.rigging.BoneDisplay
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.actor.rigging.Transform
import com.donnclab.kinetiframe.core.document.PuppetDefinition
import com.donnclab.kinetiframe.core.document.PuppetInstance
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Matrix mapping is Android-framework behavior, so this verification runs on a device. */
@RunWith(AndroidJUnit4::class)
class PuppetRenderPlanTest {
    @Test
    fun renderCommandMapsTexturePivotToBoneRestPosition() {
        val definition = PuppetDefinition(
            id = "puppet",
            name = "Puppet",
            sceneGraph = SceneGraph(
                rootBoneIds = listOf("root"),
                bones = mapOf(
                    "root" to BoneNode(
                        hierarchy = BoneHierarchy("root", "Root", null),
                        rest = BoneRest(0f, localTransform = Transform(x = 150f, y = 240f)),
                        artwork = BoneArtwork(textureBinding = TextureBinding("asset", pivotX = 20f, pivotY = 30f)),
                    ),
                ),
            ),
        )

        val command = PuppetRenderPlan.create(
            definition = definition,
            instance = PuppetInstance(id = "instance", definitionId = definition.id),
        ).single()
        val point = floatArrayOf(20f, 30f)

        command.matrix.mapPoints(point)

        assertEquals("asset", command.assetId)
        assertEquals(150f, point[0], 0.001f)
        assertEquals(240f, point[1], 0.001f)
    }

    @Test
    fun hiddenPartIsExcludedFromTheRenderPlanButStaysBound() {
        // Hiding a part is a render-time skip, not an unbind: the texture
        // binding survives so re-showing the part needs no re-attach.
        val definition = PuppetDefinition(
            id = "puppet",
            name = "Puppet",
            sceneGraph = SceneGraph(
                rootBoneIds = listOf("visible", "hidden"),
                bones = mapOf(
                    "visible" to BoneNode(
                        hierarchy = BoneHierarchy("visible", "Visible", null),
                        rest = BoneRest(0f),
                        artwork = BoneArtwork(textureBinding = TextureBinding("asset-a", pivotX = 0f, pivotY = 0f)),
                    ),
                    "hidden" to BoneNode(
                        hierarchy = BoneHierarchy("hidden", "Hidden", null),
                        rest = BoneRest(0f),
                        artwork = BoneArtwork(textureBinding = TextureBinding("asset-b", pivotX = 0f, pivotY = 0f)),
                        display = BoneDisplay(visible = false),
                    ),
                ),
            ),
        )

        val commands = PuppetRenderPlan.create(
            definition,
            PuppetInstance(id = "instance", definitionId = definition.id),
        )

        assertEquals(listOf("asset-a"), commands.map { it.assetId })
        assertEquals("asset-b", definition.sceneGraph.bones.getValue("hidden").textureBinding?.assetId)
    }
}
