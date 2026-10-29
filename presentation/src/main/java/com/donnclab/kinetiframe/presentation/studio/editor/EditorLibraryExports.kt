// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.editor

import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.ActorPlacementReference
import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import com.donnclab.kinetiframe.presentation.studio.StudioViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The editor's "Add to Library" actions. Raw assets first ask whether they are saved to the
 * project or to the personal library; the export runs once the animator chooses.
 */
internal class EditorLibraryExports(
    private val storage: EditorStorageViewModel,
    private val viewModel: StudioViewModel,
    private val ui: EditorUiState,
    private val scope: CoroutineScope,
) {
    private val messages get() = ui.messages

    fun exportBackground() {
        ui.panels.pendingLibraryExport = LibraryExportRequest("this background image") { libraryScope ->
            scope.launch {
                messages.libraryMessage = storage.exportBackground(viewModel.state.value.project, libraryScope)
                    .message("Added background image to your library")
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
                messages.libraryMessage = storage.exportFrame(frame, name, libraryScope, state.project.id)
                    .message("Added frame to your library")
            }
        }
    }

    fun exportAudioClip(clip: AudioClipPlacement) {
        ui.panels.pendingLibraryExport = LibraryExportRequest("\"${clip.label}\"") { libraryScope ->
            scope.launch {
                messages.libraryMessage = storage.exportAudioClip(clip, viewModel.state.value.project, libraryScope)
                    .message("Added \"${clip.label}\" to your library")
            }
        }
    }

    fun exportPerformance(placement: ActorPlacementReference) {
        scope.launch {
            messages.actorLibraryMessage = storage.exportPerformance(viewModel.state.value.project.id, placement)
                .message("Added ${placement.label} to your library")
        }
    }
}

private fun StoreResult<*>.message(success: String): String = when (this) {
    is StoreResult.Value -> success
    is StoreResult.Failure -> message
}
