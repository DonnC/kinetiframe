package com.donnclab.kinetiframe.engine.model

import com.donnclab.kinetiframe.engine.rigging.model.TextureBinding

/** Immutable document update plus decoded runtime assets that can now be safely released. */
data class AssetMutation(val document: ProjectDocument, val releasableAssetIds: Set<String> = emptySet())

/**
 * Every asset ID reachable from this document's puppet definitions: bound bone textures plus
 * texture-slot defaults/alternates. This is also the set used for runtime bitmap garbage
 * collection ([com.donnclab.kinetiframe.engine.rigging.texture.RigAssetManager.retainOnly]) once undo/redo
 * snapshots are folded in by the caller, so keep it in sync with every new way a definition can
 * reference an asset.
 */
fun ProjectDocument.referencedAssetIds(): Set<String> = puppetDefinitions.values.flatMap { definition ->
    val slotAssetIds = definition.textureSlots.values.flatMap { slot ->
        listOf(slot.defaultAssetId) +
            slot.alternateAssetIds
    }
    val boneAssetIds = com.donnclab.kinetiframe.engine.rigging.model.ActorArtwork.bindings(
        definition.sceneGraph,
    ).values.map {
        it.assetId
    }
    slotAssetIds + boneAssetIds
}.toSet()

/** Returns true when any definition still uses [assetId] through a slot or bone binding. */
fun ProjectDocument.isAssetReferenced(assetId: String): Boolean = assetId in referencedAssetIds()

/**
 * Rebinds every definition reference from [replacedAssetId] to [replacement].
 *
 * The operation is immutable and intentionally does not touch runtime bitmaps. The caller
 * releases the old decoded bitmap only after the returned document has no references to it.
 */
fun ProjectDocument.replaceAsset(replacedAssetId: String, replacement: ProjectAsset): ProjectDocument {
    require(replacedAssetId in assets) { "Unknown asset: $replacedAssetId" }
    require(replacement.id != replacedAssetId) { "Replacement asset must have a new ID." }

    val updatedDefinitions = puppetDefinitions.mapValues { (_, definition) ->
        definition.copy(
            textureSlots = definition.textureSlots.mapValues { (_, slot) ->
                slot.copy(
                    defaultAssetId = if (slot.defaultAssetId ==
                        replacedAssetId
                    ) {
                        replacement.id
                    } else {
                        slot.defaultAssetId
                    },
                    alternateAssetIds = slot.alternateAssetIds
                        .map { if (it == replacedAssetId) replacement.id else it }
                        .toSet(),
                )
            },
            sceneGraph = definition.sceneGraph.copy(
                bones = definition.sceneGraph.bones.mapValues { (_, bone) ->
                    val binding = bone.textureBinding
                    bone.withArtwork(
                        textureBinding = if (binding?.assetId ==
                            replacedAssetId
                        ) {
                            binding.copy(assetId = replacement.id)
                        } else {
                            binding
                        },
                        artworkVariants = bone.artworkVariants.mapValues { (_, variant) ->
                            if (variant.binding.assetId ==
                                replacedAssetId
                            ) {
                                variant.copy(binding = variant.binding.copy(assetId = replacement.id))
                            } else {
                                variant
                            }
                        },
                    )
                },
            ),
        )
    }

    return copy(
        assets = (assets - replacedAssetId) + (replacement.id to replacement),
        puppetDefinitions = updatedDefinitions,
    )
}

/** Removes [assetId] only when no definition references it. */
fun ProjectDocument.removeAssetIfUnreferenced(assetId: String): ProjectDocument =
    if (isAssetReferenced(assetId)) this else copy(assets = assets - assetId)

/**
 * Binds [binding] to one named bone and cleans up the previous attachment only when it
 * has no remaining document references. This lets definitions share a texture safely.
 */
fun ProjectDocument.bindTextureToBone(
    definitionId: String,
    boneId: String,
    slot: TextureSlot,
    binding: TextureBinding,
    asset: ProjectAsset,
): AssetMutation {
    val definition = puppetDefinitions[definitionId] ?: return AssetMutation(this)
    val bone = definition.sceneGraph.bones[boneId] ?: return AssetMutation(this)
    val previousAssetId = bone.textureBinding?.assetId
    val previousSlotId = bone.textureSlotId
    // A slot may intentionally be shared. Replacing one attachment must not silently
    // change the slot default for its sibling, so give the edited bone its own slot.
    val effectiveSlot = if (definition.sceneGraph.bones.values.any {
            it.id != boneId && it.textureSlotId == slot.id
        }
    ) {
        slot.copy(id = "${slot.id}-$boneId")
    } else {
        slot
    }
    val updatedBones =
        definition.sceneGraph.bones +
            (boneId to bone.withArtwork(textureSlotId = effectiveSlot.id, textureBinding = binding))
    // If the bone is moving off its previous slot (a different effective slot ID),
    // drop that old slot once no other bone still points at it -- otherwise it lingers
    // in textureSlots forever, keeping its defaultAssetId artificially "referenced" and
    // leaking the bitmap it points at (see ProjectAssetMutationsTest for the regression).
    val previousSlotStillUsed = previousSlotId != null &&
        (previousSlotId == effectiveSlot.id || updatedBones.values.any { it.textureSlotId == previousSlotId })
    val updatedSlots = (definition.textureSlots + (effectiveSlot.id to effectiveSlot)).let { slots ->
        if (previousSlotId == null || previousSlotStillUsed) slots else slots - previousSlotId
    }
    val updatedDefinition = definition.copy(
        sceneGraph = definition.sceneGraph.copy(bones = updatedBones),
        textureSlots = updatedSlots,
    )
    var updated = copy(
        assets = assets + (asset.id to asset),
        puppetDefinitions = puppetDefinitions + (definitionId to updatedDefinition),
    )
    val releasable = previousAssetId
        ?.takeIf { it != asset.id && !updated.isAssetReferenced(it) }
        ?.also { updated = updated.copy(assets = updated.assets - it) }
        ?.let(::setOf)
        .orEmpty()
    return AssetMutation(updated, releasable)
}

/** Detaches one bone while retaining shared slots/assets until their final reference is gone. */
fun ProjectDocument.unbindTextureFromBone(definitionId: String, boneId: String): AssetMutation {
    val definition = puppetDefinitions[definitionId] ?: return AssetMutation(this)
    val bone = definition.sceneGraph.bones[boneId] ?: return AssetMutation(this)
    val previousAssetId = bone.textureBinding?.assetId ?: return AssetMutation(this)
    val slotId = bone.textureSlotId
    val remainingBones =
        definition.sceneGraph.bones + (boneId to bone.withArtwork(textureSlotId = null, textureBinding = null))
    val slotStillUsed = slotId != null && remainingBones.values.any { it.textureSlotId == slotId }
    val updatedDefinition = definition.copy(
        sceneGraph = definition.sceneGraph.copy(bones = remainingBones),
        textureSlots = if (slotStillUsed ||
            slotId == null
        ) {
            definition.textureSlots
        } else {
            definition.textureSlots - slotId
        },
    )
    var updated = copy(puppetDefinitions = puppetDefinitions + (definitionId to updatedDefinition))
    val releasable = previousAssetId
        .takeIf { !updated.isAssetReferenced(it) }
        ?.also { updated = updated.copy(assets = updated.assets - it) }
        ?.let(::setOf)
        .orEmpty()
    return AssetMutation(updated, releasable)
}
