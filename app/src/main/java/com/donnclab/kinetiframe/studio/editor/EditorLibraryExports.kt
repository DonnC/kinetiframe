// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

import com.donnclab.kinetiframe.core.document.ActorPlacementReference
import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import com.donnclab.kinetiframe.studio.StudioViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The editor's "Add to Library" actions. Raw assets first ask whether they are saved to the
 * project or to the personal library; the export runs once the animator chooses.
 */
internal class EditorLibraryExports(
    private val actions: EditorLibraryActions,
    private val viewModel: StudioViewModel,
    private val ui: EditorUiState,
    private val scope: CoroutineScope,
) {
    private val messages get() = ui.messages

    fun exportBackground() {
        ui.panels.pendingLibraryExport = LibraryExportRequest("this background image") { libraryScope ->
            scope.launch {
                messages.libraryMessage = actions.exportBackground(viewModel.state.value.project, libraryScope)
            }
        }
    }

    fun exportCurrentFrame() {
        val frame = viewModel.state.value.currentBitmap
        if (frame == null) {
            messages.libraryError = "Nothing to save -- this frame is empty"
            return
        }
        ui.panels.pendingLibraryExport = LibraryExportRequest("this frame") { libraryScope ->
            scope.launch {
                val state = viewModel.state.value
                val name = "Frame ${state.animation.currentFrame + 1}"
                messages.libraryMessage = actions.exportFrame(frame, name, libraryScope, state.project.id)
            }
        }
    }

    fun exportAudioClip(clip: AudioClipPlacement) {
        ui.panels.pendingLibraryExport = LibraryExportRequest("\"${clip.label}\"") { libraryScope ->
            scope.launch {
                messages.libraryMessage = actions.exportAudioClip(clip, viewModel.state.value.project, libraryScope)
            }
        }
    }

    fun exportPerformance(placement: ActorPlacementReference) {
        scope.launch {
            messages.actorLibraryMessage = actions.exportPerformance(viewModel.state.value.project.id, placement)
        }
    }
}
