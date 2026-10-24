// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

import androidx.compose.runtime.Composable
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.library.ui.LibraryPickerDialog
import com.donnclab.kinetiframe.library.ui.LibraryScopeDialog
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.StudioViewModel
import com.donnclab.kinetiframe.studio.ui.GridOptionsPopup
import com.donnclab.kinetiframe.studio.ui.LayerPanelPopup
import com.donnclab.kinetiframe.studio.ui.OnionOptionsPopup
import com.donnclab.kinetiframe.studio.ui.dialogs.TextInsertionDialog
import com.donnclab.kinetiframe.studio.ui.frames.FrameStudio

/** The editor's dialogs and popups that open over the canvas. */
@Composable
internal fun EditorDialogs(
    viewModel: StudioViewModel,
    state: StudioState,
    ui: EditorUiState,
    repositories: EditorRepositories,
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
            library = repositories.library,
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
            actorRendererFactory = repositories::actorFrameRenderer,
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
