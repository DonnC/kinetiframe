// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.donnclab.kinetiframe.presentation.studio.StudioViewModel
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

/** How long editing must pause before an autosave. Long enough not to save mid-gesture. */
private const val AUTOSAVE_PAUSE_MS = 4_000L

/**
 * Saves a gallery project on its own when [enabled]: after a pause in editing and when the app goes
 * to the background. Saves wait while something is half done, such as a floating selection, a fill
 * preview or playback, so an autosave never commits or interrupts work in progress.
 */
@Composable
@OptIn(FlowPreview::class)
internal fun EditorAutosave(viewModel: StudioViewModel, saves: EditorSaveActions, enabled: Boolean) {
    if (!enabled) return
    LaunchedEffect(viewModel) {
        viewModel.state.debounce(AUTOSAVE_PAUSE_MS).collect { state ->
            if (state.isSettled()) saves.autosave()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (viewModel.state.value.isSettled()) saves.autosave()
    }
}

private fun StudioState.isSettled(): Boolean = !animation.isPlaying &&
    selection.selectedContent == null &&
    !fill.previewActive &&
    !constructionShape.previewActive &&
    scene.studioMode == StudioMode.RASTER_DRAWING
