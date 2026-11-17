// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.editor

import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.ActorPlacementReference
import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.UiText
import com.donnclab.kinetiframe.presentation.common.toUiText
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
        ui.panels.pendingLibraryExport =
            LibraryExportRequest(UiText.Resource(R.string.editor_subject_background)) { libraryScope ->
                scope.launch {
                    messages.libraryMessage = storage.exportBackground(viewModel.state.value.project, libraryScope)
                        .message(UiText.Resource(R.string.editor_added_background))
                }
            }
    }

    fun exportCurrentFrame() {
        val frame = viewModel.state.value.currentBitmap
        if (frame == null) {
            messages.libraryError = UiText.Resource(R.string.editor_frame_empty)
            return
        }
        ui.panels.pendingLibraryExport = LibraryExportRequest(
            UiText.Resource(R.string.editor_subject_frame),
        ) { libraryScope ->
            scope.launch {
                val state = viewModel.state.value
                // The saved item's name, kept as written like other library item names.
                val name = "Frame ${state.animation.currentFrame + 1}"
                messages.libraryMessage = storage.exportFrame(frame, name, libraryScope, state.project.id)
                    .message(UiText.Resource(R.string.editor_added_frame))
            }
        }
    }

    fun exportAudioClip(clip: AudioClipPlacement) {
        val subject = UiText.Resource(R.string.editor_subject_named, listOf(clip.label))
        ui.panels.pendingLibraryExport = LibraryExportRequest(subject) { libraryScope ->
            scope.launch {
                messages.libraryMessage = storage.exportAudioClip(clip, viewModel.state.value.project, libraryScope)
                    .message(UiText.Resource(R.string.editor_added_audio, listOf(clip.label)))
            }
        }
    }

    fun exportPerformance(placement: ActorPlacementReference) {
        scope.launch {
            messages.actorLibraryMessage = storage.exportPerformance(viewModel.state.value.project.id, placement)
                .message(UiText.Resource(R.string.editor_added_performance, listOf(placement.label)))
        }
    }
}

private fun StoreResult<*>.message(success: UiText): UiText = when (this) {
    is StoreResult.Value -> success
    is StoreResult.Failure -> toUiText()
}
