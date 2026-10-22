package com.donnclab.kinetiframe.actors.render

import com.donnclab.kinetiframe.actors.rig.model.BoneNode
import com.donnclab.kinetiframe.actors.rig.model.SceneGraph
import com.donnclab.kinetiframe.actors.rig.model.TextureBinding
import com.donnclab.kinetiframe.actors.rig.model.Transform
import com.donnclab.kinetiframe.core.actor.rigging.BoneArtwork
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.project.model.PuppetDefinition
import com.donnclab.kinetiframe.project.model.PuppetInstance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PuppetRenderPlanTest {
    @Test
    fun `creates one render command for each isolated bound body part`() {
        val torso = BoneNode(
            hierarchy = BoneHierarchy("torso", "Torso", null),
            rest = BoneRest(100f, localTransform = Transform(x = 200f, y = 200f)),
            artwork = BoneArtwork(textureBinding = TextureBinding(assetId = "torso-art", pivotX = 20f, pivotY = 20f)),
        )
        val forearm = BoneNode(
            hierarchy = BoneHierarchy("forearm", "Forearm", "torso"),
            rest = BoneRest(80f, localTransform = Transform(x = 100f)),
            artwork = BoneArtwork(textureBinding = TextureBinding(assetId = "forearm-art", pivotX = 12f, pivotY = 12f)),
        )
        val definition = PuppetDefinition(
            id = "hero",
            name = "Hero",
            sceneGraph = SceneGraph(
                rootBoneIds = listOf("torso"),
                bones = mapOf("torso" to torso, "forearm" to forearm),
            ),
        )

        val commands = PuppetRenderPlan.create(definition, PuppetInstance(id = "hero-instance", definitionId = "hero"))

        assertEquals(listOf("torso-art", "forearm-art"), commands.map { it.assetId })
        assertEquals(2, commands.size)
    }
}
