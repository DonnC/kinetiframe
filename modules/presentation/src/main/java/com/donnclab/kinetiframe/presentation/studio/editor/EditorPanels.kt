// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.editor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.donnclab.kinetiframe.core.actor.document.LibraryScope
import com.donnclab.kinetiframe.presentation.common.UiText

/** An "Add to Library" request waiting for the animator to choose where the item is saved. */
internal data class LibraryExportRequest(val itemLabel: UiText, val export: (LibraryScope) -> Unit)

/** Which of the editor's panels, sheets, menus and dialogs are open. */
@Stable
internal class EditorPanels {
    /** False in focus mode, which hides the bars and docks so only the canvas shows. */
    var toolbarVisible by mutableStateOf(true)
    var tracksWorkspaceVisible by mutableStateOf(false)
    var layerSheetVisible by mutableStateOf(false)

    /** The tablet workspace's layers panel starts closed, so the canvas gets the room; the
     *  timeline's layers button opens it beside the canvas. */
    var workspaceLayersOpen by mutableStateOf(false)

    /** Showing the audience's framing through the camera instead of editing artwork. Shared by the
     *  canvas and the camera controls, which sit on the canvas on a phone and in the top bar on a tablet. */
    val cameraPreview =
        mutableStateOf(com.donnclab.kinetiframe.presentation.studio.camera.CameraWorkspacePolicy.PREVIEW_ON_OPEN)
    var frameStudioVisible by mutableStateOf(false)
    var showOverflowMenu by mutableStateOf(false)
    var showGridMenu by mutableStateOf(false)
    var showOnionMenu by mutableStateOf(false)
    var showPartPicker by mutableStateOf(false)
    var showPresetLibrary by mutableStateOf(false)
    var showLibraryPicker by mutableStateOf(false)

    /** The Library picker was opened to choose a background, so it shows background images only. */
    var libraryPickerBackgroundsOnly by mutableStateOf(false)
    var showRecordAudioDialog by mutableStateOf(false)

    /** Stage position for a new text node. */
    var textAddDialog by mutableStateOf<Pair<Float, Float>?>(null)

    /** Text node ID and its current text. */
    var textEditDialog by mutableStateOf<Pair<String, String>?>(null)

    /** Pack Creator's item details: the frame index, and whether a new item is being created there. */
    var packItemDetails by mutableStateOf<Pair<Int, Boolean>?>(null)
    var pendingLibraryExport by mutableStateOf<LibraryExportRequest?>(null)
}
