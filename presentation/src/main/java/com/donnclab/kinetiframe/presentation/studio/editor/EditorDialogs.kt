// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.editor

import androidx.compose.runtime.Composable
import com.donnclab.kinetiframe.actors.render.ActorRendering
import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.presentation.library.LibraryPickerDialog
import com.donnclab.kinetiframe.presentation.library.LibraryScopeDialog
import com.donnclab.kinetiframe.presentation.studio.GridOptionsPopup
import com.donnclab.kinetiframe.presentation.studio.LayerPanelPopup
import com.donnclab.kinetiframe.presentation.studio.OnionOptionsPopup
import com.donnclab.kinetiframe.presentation.studio.StudioViewModel
import com.donnclab.kinetiframe.presentation.studio.dialogs.TextInsertionDialog
import com.donnclab.kinetiframe.presentation.studio.frames.FrameStudio
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState

/**
 * What the editor's "From Library" item can pick from. PROJECT is left out because a project is
 * imported from the gallery, not into another open project. BUNDLE is left out because no screen
 * creates bundles yet. A PACK opens the pack tray instead of being placed.
 */
internal val EditorLibraryKinds = setOf(
    LibraryItemKind.ACTOR,
    LibraryItemKind.PERFORMANCE,
    LibraryItemKind.AUDIO,
    LibraryItemKind.BACKGROUND_IMAGE,
    LibraryItemKind.FRAME,
    LibraryItemKind.PACK,
)

/** The editor's dialogs and popups that open over the canvas. */
@Composable
internal fun EditorDialogs(
    viewModel: StudioViewModel,
    state: StudioState,
    ui: EditorUiState,
    actorRendering: ActorRendering,
    onLibraryPick: (LibraryItemManifest) -> Unit,
) {
    val panels = ui.panels
    val onIntent: (StudioIntent) -> Unit = viewModel::handleIntent
    panels.textAddDialog?.let { (x, y) ->
        TextInsertionDialog(
            onDismiss = { panels.textAddDialog = null },
            onConfirm = { text ->
                onIntent(StudioIntent.Text.AddText(text, x, y))
                panels.textAddDialog = null
            },
        )
    }
    panels.textEditDialog?.let { (id, initialText) ->
        TextInsertionDialog(
            initialText = initialText,
            onDismiss = { panels.textEditDialog = null },
            onConfirm = { text ->
                onIntent(StudioIntent.Text.UpdateText(id, text))
                panels.textEditDialog = null
            },
        )
    }
    EditorMessageDialogs(ui.messages)
    panels.pendingLibraryExport?.let { request ->
        LibraryScopeDialog(
            itemLabel = request.itemLabel,
            onDismiss = { panels.pendingLibraryExport = null },
            onChoose = { chosenScope ->
                panels.pendingLibraryExport = null
                request.export(chosenScope)
            },
        )
    }
    if (panels.showLibraryPicker) {
        LibraryPickerDialog(
            allowedKinds = EditorLibraryKinds,
            projectId = state.project.id,
            onDismiss = { panels.showLibraryPicker = false },
            onOpenPack = { manifest -> ui.tray.manifest = manifest },
            onPick = onLibraryPick,
        )
    }
    PackTrayErrorDialog(ui.tray)
    if (panels.frameStudioVisible) {
        FrameStudio(
            actorRendererFactory = actorRendering::newRenderer,
            state = state,
            cacheManager = viewModel.cacheManager,
            onIntent = onIntent,
            onOpenFrame = { frame ->
                onIntent(StudioIntent.Playback.SetCurrentFrame(frame))
                panels.frameStudioVisible = false
            },
            onDismiss = { panels.frameStudioVisible = false },
        )
    }
    if (panels.layerSheetVisible) {
        LayerPanelPopup(
            state = state,
            onIntent = onIntent,
            cacheManager = viewModel.cacheManager,
            onDismiss = { panels.layerSheetVisible = false },
        )
    }
    if (panels.showGridMenu) {
        GridOptionsPopup(state = state, onIntent = onIntent, onDismiss = { panels.showGridMenu = false })
    }
    if (panels.showOnionMenu) {
        OnionOptionsPopup(state = state, onIntent = onIntent, onDismiss = { panels.showOnionMenu = false })
    }
}
