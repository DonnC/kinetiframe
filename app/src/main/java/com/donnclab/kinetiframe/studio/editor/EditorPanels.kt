// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.donnclab.kinetiframe.actors.data.LibraryScope

/** An "Add to Library" request waiting for the animator to choose where the item is saved. */
internal data class LibraryExportRequest(val itemLabel: String, val export: (LibraryScope) -> Unit)

/** Which of the editor's panels, sheets, menus and dialogs are open. */
@Stable
internal class EditorPanels {
    /** False in focus mode, which hides the bars and docks so only the canvas shows. */
    var toolbarVisible by mutableStateOf(true)
    var tracksWorkspaceVisible by mutableStateOf(false)
    var layerSheetVisible by mutableStateOf(false)
    var frameStudioVisible by mutableStateOf(false)
    var showOverflowMenu by mutableStateOf(false)
    var showGridMenu by mutableStateOf(false)
    var showOnionMenu by mutableStateOf(false)
    var showPartPicker by mutableStateOf(false)
    var showPresetLibrary by mutableStateOf(false)
    var showLibraryPicker by mutableStateOf(false)
    var showRecordAudioDialog by mutableStateOf(false)

    /** Stage position for a new text node. */
    var textAddDialog by mutableStateOf<Pair<Float, Float>?>(null)

    /** Text node ID and its current text. */
    var textEditDialog by mutableStateOf<Pair<String, String>?>(null)

    /** Pack Creator's item details: the frame index, and whether a new item is being created there. */
    var packItemDetails by mutableStateOf<Pair<Int, Boolean>?>(null)
    var pendingLibraryExport by mutableStateOf<LibraryExportRequest?>(null)
}
