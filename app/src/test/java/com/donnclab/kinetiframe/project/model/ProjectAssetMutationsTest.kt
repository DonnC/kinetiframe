package com.donnclab.kinetiframe.project.model

import com.donnclab.kinetiframe.core.actor.artwork.ArtworkVariant
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.actor.rigging.BoneArtwork
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.document.AssetKind
import com.donnclab.kinetiframe.core.document.ProjectAsset
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.PuppetDefinition
import com.donnclab.kinetiframe.core.document.TextureSlot
import com.donnclab.kinetiframe.core.document.bindTextureToBone
import com.donnclab.kinetiframe.core.document.isAssetReferenced
import com.donnclab.kinetiframe.core.document.referencedAssetIds
import com.donnclab.kinetiframe.core.document.replaceAsset
import com.donnclab.kinetiframe.core.document.unbindTextureFromBone
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProjectAssetMutationsTest {
    @Test fun `variant artwork stays reachable and replacement updates every binding`() {
        val bone = BoneNode(
            hierarchy = BoneHierarchy("mouth", "Mouth", null),
            rest = BoneRest(10f),
            artwork = BoneArtwork(
                artworkVariants = mapOf(
                    "smile" to
                        ArtworkVariant(
                            "Smile",
                            TextureBinding("old", 2f, 3f),
                        ),
                ),
            ),
        )
        val definition = PuppetDefinition("actor", "Actor", SceneGraph(listOf("mouth"), mapOf("mouth" to bone)))
        val document =
            ProjectDocument(
                assets = mapOf("old" to ProjectAsset("old", AssetKind.RASTER_TEXTURE, width = 4, height = 4)),
                puppetDefinitions = mapOf("actor" to definition),
            )
        assertEquals(setOf("old"), document.referencedAssetIds())
        val replaced = document.replaceAsset(
            "old",
            ProjectAsset("new", AssetKind.RASTER_TEXTURE, width = 4, height = 4),
        )
        assertEquals(setOf("new"), replaced.referencedAssetIds())
        assertEquals(
            2f,
            replaced.puppetDefinitions.getValue(
                "actor",
            ).sceneGraph.bones.getValue("mouth").artworkVariants.getValue("smile").binding.pivotX,
        )
    }

    private fun boneWithTexture(id: String, assetId: String, slotId: String = "slot-$id") = BoneNode(
        hierarchy = BoneHierarchy(id, id, null),
        rest = BoneRest(10f),
        artwork = BoneArtwork(
            textureSlotId = slotId,
            textureBinding = TextureBinding(assetId = assetId, pivotX = 0f, pivotY = 0f),
        ),
    )

    private fun documentWithBoundBone(definitionId: String, boneId: String, assetId: String): ProjectDocument {
        val bone = boneWithTexture(boneId, assetId)
        val definition = PuppetDefinition(
            id = definitionId,
            name = "Actor",
            sceneGraph = SceneGraph(rootBoneIds = listOf(boneId), bones = mapOf(boneId to bone)),
            textureSlots = mapOf(bone.textureSlotId!! to TextureSlot(bone.textureSlotId!!, assetId)),
        )
        return ProjectDocument(puppetDefinitions = mapOf(definitionId to definition))
    }

    @Test
    fun `referencedAssetIds includes both bone bindings and slot defaults`() {
        val document = documentWithBoundBone("actor-1", "torso", "tex-1")

        assertEquals(setOf("tex-1"), document.referencedAssetIds())
        assertTrue(document.isAssetReferenced("tex-1"))
        assertFalse(document.isAssetReferenced("tex-unrelated"))
    }

    @Test
    fun `unbindTextureFromBone reports the asset releasable only once no reference remains`() {
        val document = documentWithBoundBone("actor-1", "torso", "tex-1")

        val mutation = document.unbindTextureFromBone("actor-1", "torso")

        assertEquals(setOf("tex-1"), mutation.releasableAssetIds)
        assertFalse(mutation.document.isAssetReferenced("tex-1"))
        assertTrue(mutation.document.referencedAssetIds().isEmpty())
    }

    @Test
    fun `unbindTextureFromBone keeps an asset shared by a sibling bone reachable`() {
        val shared = "tex-shared"
        val boneA = boneWithTexture("left_hand", shared, slotId = "slot-shared")
        val boneB = boneWithTexture("right_hand", shared, slotId = "slot-shared")
        val definition = PuppetDefinition(
            id = "actor-1",
            name = "Actor",
            sceneGraph = SceneGraph(
                rootBoneIds = listOf(boneA.id, boneB.id),
                bones = mapOf(
                    boneA.id to boneA,
                    boneB.id to boneB,
                ),
            ),
            textureSlots = mapOf("slot-shared" to TextureSlot("slot-shared", shared)),
        )
        val document = ProjectDocument(puppetDefinitions = mapOf("actor-1" to definition))

        val mutation = document.unbindTextureFromBone("actor-1", "left_hand")

        // The other hand still binds the same texture, so it must not be reported releasable.
        assertTrue(mutation.releasableAssetIds.isEmpty())
        assertTrue(mutation.document.isAssetReferenced(shared))
        assertEquals(setOf(shared), mutation.document.referencedAssetIds())
    }

    @Test
    fun `bindTextureToBone reports the previous asset releasable only when it becomes unreferenced`() {
        val document = documentWithBoundBone("actor-1", "torso", "tex-old")
        val newAsset = ProjectAsset("tex-new", AssetKind.RASTER_TEXTURE, width = 4, height = 4)

        val mutation = document.bindTextureToBone(
            definitionId = "actor-1",
            boneId = "torso",
            slot = TextureSlot("slot-new", newAsset.id),
            binding = TextureBinding(assetId = newAsset.id, pivotX = 0f, pivotY = 0f),
            asset = newAsset,
        )

        assertEquals(setOf("tex-old"), mutation.releasableAssetIds)
        assertEquals(setOf("tex-new"), mutation.document.referencedAssetIds())
    }
}
