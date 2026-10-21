package com.donnclab.kinetiframe.ui.actors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import com.donnclab.kinetiframe.engine.rigging.model.BoneNode
import com.donnclab.kinetiframe.engine.rigging.texture.RigAssetManager
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.components.rigging.ArtworkAlignmentDialog

/** Borrows owned runtime pixels; dismissing this editor must never recycle actor/undo artwork. */
@Composable
fun ActorArtworkEditor(
    bone: BoneNode,
    variantId: String,
    assets: RigAssetManager,
    onDismiss: () -> Unit,
    onIntent: (TimelineIntent) -> Unit,
    graph: com.donnclab.kinetiframe.engine.rigging.model.SceneGraph? = null,
) {
    val binding = if (variantId.isEmpty()) bone.textureBinding else bone.artworkVariants[variantId]?.binding
    val bitmap = binding?.let { assets.getAsset(it.assetId) }
    if (binding == null || bitmap == null || bitmap.isRecycled) {
        LaunchedEffect(bone.id, variantId) { onDismiss() }
        return
    }
    key(bone.id, variantId) {
        ArtworkAlignmentDialog(
            bone.artworkVariants[variantId]?.name ?: bone.name, bitmap, bone.length,
            graph?.let { com.donnclab.kinetiframe.engine.rigging.math.RigGeometry.resolve(it)[bone.id]?.angle } ?: 0f,
            onDismiss,
            onAttach = { name, alignment, _, _ ->
                onIntent(TimelineIntent.Actor.UpdateArtwork(bone.id, variantId, name, alignment))
                onDismiss()
            },
            initialBinding = binding,
            editing = true,
            showName = variantId.isNotEmpty(),
            graph = graph,
            partId = bone.id,
            assets = assets,
        )
    }
}
