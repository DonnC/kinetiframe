// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.donnclab.kinetiframe.presentation.studio.StudioViewModel
import com.donnclab.kinetiframe.presentation.studio.actorstudio.ActorArtworkAttachmentFlow
import com.donnclab.kinetiframe.presentation.studio.actorstudio.ActorArtworkEditor
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.actorDefinition
import kotlinx.coroutines.CancellationException

/** Actor Studio's artwork flows: attaching, replacing and editing a part's artwork, and the sketch editor. */
@Stable
internal class ActorArtworkState {
    /** The part whose artwork is being attached. */
    var partId by mutableStateOf<String?>(null)
    var asVariant by mutableStateOf(false)

    /** The variant the attached artwork replaces, if any. */
    var replacement by mutableStateOf<String?>(null)

    /** Part ID and variant ID of the artwork open in the editor. */
    var editing by mutableStateOf<Pair<String, String>?>(null)
    var sketchOpen by mutableStateOf(false)
    private var openingSketch by mutableStateOf(false)

    /** Starts attaching artwork to the selected part. Does nothing when no part is selected. */
    fun choose(asVariant: Boolean, state: StudioState, onIntent: (StudioIntent) -> Unit) {
        replacement = null
        state.scene.selectedBoneId?.let { part ->
            onIntent(StudioIntent.Rigging.SetStudioMode(StudioMode.RIG_SETUP))
            this.asVariant = asVariant
            partId = part
        }
    }

    /** Starts attaching artwork that replaces [variantId], or adds a new one when it is empty. */
    fun replace(variantId: String, state: StudioState, onIntent: (StudioIntent) -> Unit) {
        choose(false, state, onIntent)
        replacement = variantId.takeIf(String::isNotEmpty)
    }

    /** Saves pending canvas work, then opens the whole-actor sketch editor. */
    fun openSketch(viewModel: StudioViewModel, messages: EditorMessages) {
        if (openingSketch) return
        openingSketch = true
        viewModel.launchSave {
            try {
                viewModel.prepareProjectSave()
                viewModel.handleIntent(StudioIntent.Rigging.SetStudioMode(StudioMode.RIG_SETUP))
                sketchOpen = true
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                viewModel.handleIntent(StudioIntent.Rigging.SetStudioMode(StudioMode.RIG_SETUP))
                messages.projectSaveMessage = failure.message ?: "Could not open drawing"
            } finally {
                openingSketch = false
            }
        }
    }
}

@Composable
internal fun ActorArtworkFlows(artwork: ActorArtworkState, state: StudioState, viewModel: StudioViewModel) {
    val graph = state.actorDefinition()?.sceneGraph
    artwork.partId?.let { part ->
        graph?.let {
            ActorArtworkAttachmentFlow(
                part,
                it,
                { artwork.partId = null },
                viewModel::handleIntent,
                initialAsVariant = artwork.asVariant,
                replaceVariantId = artwork.replacement,
                assets = viewModel.rigAssetManager,
            )
        }
    }
    artwork.editing?.let { (part, variant) ->
        graph?.bones?.get(part)?.let { bone ->
            ActorArtworkEditor(
                bone,
                variant,
                viewModel.rigAssetManager,
                { artwork.editing = null },
                viewModel::handleIntent,
                graph = graph,
            )
        }
    }
}
