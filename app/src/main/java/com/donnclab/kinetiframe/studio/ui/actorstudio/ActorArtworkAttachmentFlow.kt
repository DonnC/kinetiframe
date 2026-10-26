package com.donnclab.kinetiframe.studio.ui.actorstudio

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.donnclab.kinetiframe.actors.rig.math.RigGeometry
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.presentation.actors.rigging.ArtworkAlignmentDialog
import com.donnclab.kinetiframe.studio.StudioIntent

/** Owns an uncommitted decoded item until Cancel or the MVI attachment transaction takes it. */
@Composable
fun ActorArtworkAttachmentFlow(
    partId: String,
    graph: SceneGraph,
    onDismiss: () -> Unit,
    onIntent: (StudioIntent) -> Unit,
    initialAsVariant: Boolean = false,
    replaceVariantId: String? = null,
    assets: com.donnclab.kinetiframe.actors.rig.texture.RigAssetManager? = null,
) {
    var selected by remember(partId) { mutableStateOf<Pair<String, Bitmap>?>(null) }
    var registration by remember(partId) {
        mutableStateOf<com.donnclab.kinetiframe.library.model.PackArtworkRegistration?>(null)
    }
    val latest by rememberUpdatedState(selected)
    var transferred by remember(partId) { mutableStateOf(false) }
    DisposableEffect(partId) { onDispose { if (!transferred) latest?.second?.takeUnless { it.isRecycled }?.recycle() } }
    val item = selected
    if (item ==
        null
    ) {
        ActorArtworkPicker(onDismiss) { name, bitmap, source ->
            selected =
                name to bitmap
            registration = source
        }
    } else {
        val bone = graph.bones[partId]
        if (bone !=
            null
        ) {
            ArtworkAlignmentDialog(
                item.first, item.second, bone.length, RigGeometry.resolve(graph).getValue(partId).angle,
                onDismiss = onDismiss, onAttach = { name, alignment, asVariant, child ->
                    transferred = true
                    onIntent(
                        StudioIntent.Actor.AttachArtwork(
                            partId,
                            name,
                            item.second,
                            alignment,
                            asVariant || replaceVariantId != null,
                            child,
                            replaceVariantId,
                            attachmentBlend =
                            child == null && bone.parentId != null &&
                                registration?.takeIf { it.partId == partId }?.attachmentBlend == true,
                        ),
                    )
                    onDismiss()
                },
                continuousChildren = graph.bones.values.filter {
                    it.parentId == partId &&
                        kotlin.math.abs(it.localTransform.x - bone.length) < .1f &&
                        kotlin.math.abs(it.localTransform.y) < .1f
                }
                    .map { it.id to it.name },
                initialAsVariant = initialAsVariant, targetPartName = bone.name,
                allowAlternate = replaceVariantId == null, graph = graph, partId = partId, assets = assets,
                initialBinding = registration?.alignment(partId, bone.length, item.second.width, item.second.height),
                initialContinuousChildId = registration?.takeIf { it.partId == partId }?.continuousChildId,
            )
        }
    }
}
