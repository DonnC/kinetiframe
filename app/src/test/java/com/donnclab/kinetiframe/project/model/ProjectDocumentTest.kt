package com.donnclab.kinetiframe.project.model

import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.actor.rigging.BoneArtwork
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.document.AssetKind
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.core.document.LayerType
import com.donnclab.kinetiframe.core.document.ProjectAsset
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.PuppetDefinition
import com.donnclab.kinetiframe.core.document.PuppetInstance
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.core.document.TextureSlot
import com.donnclab.kinetiframe.core.document.bindTextureToBone
import com.donnclab.kinetiframe.core.document.replaceAsset
import com.donnclab.kinetiframe.core.document.unbindTextureFromBone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectDocumentTest {
    @Test
    fun `stage dimensions preserve each supported project preset`() {
        assertEquals(1920, StageDimensions.LandscapeHd.width)
        assertEquals(1080, StageDimensions.LandscapeHd.height)
        assertEquals(1080, StageDimensions.Square.width)
        assertEquals(1920, StageDimensions.Vertical.height)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `stage dimensions reject invalid bitmap allocation size`() {
        StageDimensions(width = 0, height = 1080)
    }

    @Test
    fun `puppet layers resolve only their referenced instance`() {
        val layer = Layer("Hero", type = LayerType.PUPPET, puppetInstanceId = "hero-instance")
        val document = ProjectDocument(
            layers = listOf(layer),
            puppetDefinitions = mapOf("hero" to PuppetDefinition(id = "hero", name = "Hero")),
            puppetInstances = mapOf(
                "hero-instance" to PuppetInstance(id = "hero-instance", definitionId = "hero"),
            ),
        )

        assertEquals("hero", document.instanceForLayer(layer)?.definitionId)
        assertNull(document.instanceForLayer(Layer("Paint")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `project document rejects unsupported preview fps`() {
        ProjectDocument(fps = 31)
    }

    @Test
    fun `definition stores a texture slot separately from its bound bone`() {
        val asset = ProjectAsset("hand-open", AssetKind.RASTER_TEXTURE, width = 100, height = 80)
        val definition = PuppetDefinition(
            id = "hero",
            name = "Hero",
            sceneGraph = SceneGraph(
                rootBoneIds = listOf("hand"),
                bones = mapOf(
                    "hand" to BoneNode(
                        hierarchy = BoneHierarchy("hand", "Hand", null),
                        rest = BoneRest(80f),
                        artwork = BoneArtwork(textureSlotId = "hand-slot"),
                    ),
                ),
            ),
            textureSlots = mapOf("hand-slot" to TextureSlot("hand-slot", defaultAssetId = asset.id)),
        )

        assertEquals(asset.id, definition.textureSlots.getValue("hand-slot").defaultAssetId)
        assertEquals("hand-slot", definition.sceneGraph.bones.getValue("hand").textureSlotId)
    }

    @Test
    fun `asset replacement updates slots and bindings without mutating the original document`() {
        val oldAsset = ProjectAsset("hand-open", AssetKind.RASTER_TEXTURE, width = 100, height = 80)
        val newAsset = ProjectAsset("hand-fist", AssetKind.RASTER_TEXTURE, width = 100, height = 80)
        val definition = PuppetDefinition(
            id = "hero",
            name = "Hero",
            sceneGraph = SceneGraph(
                rootBoneIds = listOf("hand"),
                bones = mapOf(
                    "hand" to BoneNode(
                        hierarchy = BoneHierarchy("hand", "Hand", null),
                        rest = BoneRest(80f),
                        artwork = BoneArtwork(textureBinding = TextureBinding(oldAsset.id, 50f, 40f)),
                    ),
                ),
            ),
            textureSlots = mapOf("hand-slot" to TextureSlot("hand-slot", oldAsset.id)),
        )
        val document = ProjectDocument(
            assets = mapOf(oldAsset.id to oldAsset),
            puppetDefinitions = mapOf(definition.id to definition),
        )

        val replaced = document.replaceAsset(oldAsset.id, newAsset)

        assertTrue(oldAsset.id in document.assets)
        assertEquals(
            oldAsset.id,
            document.puppetDefinitions.getValue("hero").textureSlots.getValue("hand-slot").defaultAssetId,
        )
        assertEquals(
            newAsset.id,
            replaced.puppetDefinitions.getValue("hero").textureSlots.getValue("hand-slot").defaultAssetId,
        )
        assertEquals(
            newAsset.id,
            replaced.puppetDefinitions.getValue("hero").sceneGraph.bones.getValue("hand").textureBinding?.assetId,
        )
        assertTrue(newAsset.id in replaced.assets)
    }

    @Test
    fun `unbinding retains a shared texture until its final bone reference is removed`() {
        val asset = ProjectAsset("shared", AssetKind.RASTER_TEXTURE, width = 20, height = 20)
        val binding = TextureBinding(asset.id, 10f, 10f)
        val definition = PuppetDefinition(
            id = "hero",
            name = "Hero",
            sceneGraph = SceneGraph(
                rootBoneIds = listOf("left", "right"),
                bones = mapOf(
                    "left" to BoneNode(
                        hierarchy = BoneHierarchy("left", "Left", null),
                        rest = BoneRest(10f),
                        artwork = BoneArtwork(textureSlotId = "hands", textureBinding = binding),
                    ),
                    "right" to BoneNode(
                        hierarchy = BoneHierarchy("right", "Right", null),
                        rest = BoneRest(10f),
                        artwork = BoneArtwork(textureSlotId = "hands", textureBinding = binding),
                    ),
                ),
            ),
            textureSlots = mapOf("hands" to TextureSlot("hands", asset.id)),
        )
        val document =
            ProjectDocument(assets = mapOf(asset.id to asset), puppetDefinitions = mapOf("hero" to definition))

        val first = document.unbindTextureFromBone("hero", "left")
        assertTrue(first.releasableAssetIds.isEmpty())
        assertTrue(asset.id in first.document.assets)
        assertTrue("hands" in first.document.puppetDefinitions.getValue("hero").textureSlots)

        val final = first.document.unbindTextureFromBone("hero", "right")
        assertEquals(setOf(asset.id), final.releasableAssetIds)
        assertFalse(asset.id in final.document.assets)
        assertFalse("hands" in final.document.puppetDefinitions.getValue("hero").textureSlots)
    }

    @Test
    fun `binding a replacement releases only an orphaned previous bitmap`() {
        val old = ProjectAsset("old", AssetKind.RASTER_TEXTURE, width = 20, height = 20)
        val replacement = ProjectAsset("new", AssetKind.RASTER_TEXTURE, width = 24, height = 24)
        val definition = PuppetDefinition(
            id = "hero",
            name = "Hero",
            sceneGraph = SceneGraph(
                rootBoneIds = listOf("hand"),
                bones = mapOf(
                    "hand" to BoneNode(
                        hierarchy = BoneHierarchy("hand", "Hand", null),
                        rest = BoneRest(10f),
                        artwork = BoneArtwork(
                            textureSlotId = "hand",
                            textureBinding = TextureBinding(old.id, 10f, 10f),
                        ),
                    ),
                ),
            ),
            textureSlots = mapOf("hand" to TextureSlot("hand", old.id)),
        )
        val document = ProjectDocument(assets = mapOf(old.id to old), puppetDefinitions = mapOf("hero" to definition))

        val result = document.bindTextureToBone(
            definitionId = "hero",
            boneId = "hand",
            slot = TextureSlot("hand", replacement.id),
            binding = TextureBinding(replacement.id, 12f, 12f),
            asset = replacement,
        )

        assertEquals(setOf(old.id), result.releasableAssetIds)
        assertFalse(old.id in result.document.assets)
        assertEquals(
            replacement.id,
            result.document.puppetDefinitions.getValue("hero")
                .sceneGraph.bones.getValue("hand").textureBinding?.assetId,
        )
    }

    @Test
    fun `rebinding a member of a shared slot gives it an isolated slot`() {
        val old = ProjectAsset("old", AssetKind.RASTER_TEXTURE, width = 20, height = 20)
        val replacement = ProjectAsset("new", AssetKind.RASTER_TEXTURE, width = 24, height = 24)
        val shared = TextureBinding(old.id, 10f, 10f)
        val definition = PuppetDefinition(
            id = "hero",
            name = "Hero",
            sceneGraph = SceneGraph(
                rootBoneIds = listOf("left", "right"),
                bones = mapOf(
                    "left" to BoneNode(
                        hierarchy = BoneHierarchy("left", "Left", null),
                        rest = BoneRest(10f),
                        artwork = BoneArtwork(textureSlotId = "hands", textureBinding = shared),
                    ),
                    "right" to BoneNode(
                        hierarchy = BoneHierarchy("right", "Right", null),
                        rest = BoneRest(10f),
                        artwork = BoneArtwork(textureSlotId = "hands", textureBinding = shared),
                    ),
                ),
            ),
            textureSlots = mapOf("hands" to TextureSlot("hands", old.id)),
        )
        val document = ProjectDocument(assets = mapOf(old.id to old), puppetDefinitions = mapOf("hero" to definition))

        val result = document.bindTextureToBone(
            "hero",
            "left",
            TextureSlot("hands", replacement.id),
            TextureBinding(replacement.id, 12f, 12f),
            replacement,
        )
        val updated = result.document.puppetDefinitions.getValue("hero")
        assertEquals("hands-left", updated.sceneGraph.bones.getValue("left").textureSlotId)
        assertEquals("hands", updated.sceneGraph.bones.getValue("right").textureSlotId)
        assertEquals(old.id, updated.textureSlots.getValue("hands").defaultAssetId)
        assertEquals(replacement.id, updated.textureSlots.getValue("hands-left").defaultAssetId)
        assertTrue(old.id in result.document.assets)
        assertTrue(result.releasableAssetIds.isEmpty())
    }
}
