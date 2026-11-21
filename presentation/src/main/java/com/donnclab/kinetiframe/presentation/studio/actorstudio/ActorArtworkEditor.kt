package com.donnclab.kinetiframe.presentation.studio.actorstudio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import com.donnclab.kinetiframe.actors.rig.math.RigGeometry
import com.donnclab.kinetiframe.actors.rig.texture.RigAssetManager
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.presentation.actors.rigging.ArtworkAlignmentDialog
import com.donnclab.kinetiframe.studio.StudioIntent

/** Borrows owned runtime pixels; dismissing this editor must never recycle actor/undo artwork. */
@Composable
fun ActorArtworkEditor(
    bone: BoneNode,
    variantId: String,
    assets: RigAssetManager,
    onDismiss: () -> Unit,
    onIntent: (StudioIntent) -> Unit,
    graph: SceneGraph? = null,
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
            graph?.let { RigGeometry.resolve(it)[bone.id]?.angle } ?: 0f,
            onDismiss,
            onAttach = { name, alignment, _, _ ->
                onIntent(StudioIntent.Actor.UpdateArtwork(bone.id, variantId, name, alignment))
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
