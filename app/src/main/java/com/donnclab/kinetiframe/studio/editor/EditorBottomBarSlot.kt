// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.StudioViewModel
import com.donnclab.kinetiframe.studio.ui.timeline.ActorPerformancePicker
import com.donnclab.kinetiframe.studio.ui.timeline.TimelineControls
import com.donnclab.kinetiframe.studio.ui.timeline.TimelineTracksPanel
import com.donnclab.kinetiframe.studio.ui.timeline.TracksWorkspaceDialog

/** The timeline under the canvas, and the tracks workspace it opens. Hidden in puppet modes and focus mode. */
@Composable
internal fun EditorBottomBarSlot(
    viewModel: StudioViewModel,
    state: StudioState,
    host: EditorHost,
    ui: EditorUiState,
    repositories: EditorRepositories,
    commands: EditorCommands,
) {
    val panels = ui.panels
    if (state.scene.studioMode != StudioMode.RASTER_DRAWING || !panels.toolbarVisible) return
    val isPackCreator = host.isPackCreator
    // The Scaffold's window insets are zeroed so the canvas stays edge to edge, so nothing
    // reacts to the keyboard by default. Pack Creator's name and description fields live in
    // this bar and need their own IME padding.
    Column(Modifier.fillMaxWidth().imePadding()) {
        // Pack Creator drafts are raster drawing only: no actor, audio or video tracks.
        if (!isPackCreator && panels.tracksWorkspaceVisible) {
            TracksWorkspace(state, host, ui, repositories, commands)
        }
        TimelineControls(
            actorRendererFactory = repositories::actorFrameRenderer,
            state = state,
            onIntent = { intent ->
                when {
                    isPackCreator && intent is StudioIntent.Frame.AddFrameAt ->
                        panels.packItemDetails = intent.index to true

                    isPackCreator && intent == StudioIntent.Frame.AddFrame ->
                        panels.packItemDetails = (state.animation.currentFrame + 1) to true

                    else -> commands.onIntent(intent)
                }
            },
            cacheManager = viewModel.cacheManager,
            onLayerPanelToggle = { panels.layerSheetVisible = !panels.layerSheetVisible },
            // Shared with the track lanes so scrolling either one keeps the other in step.
            listState = ui.timelineScrollState,
            packMode = isPackCreator,
            onAddPackItem = host.packCreatorConfig?.let {
                { panels.packItemDetails = state.animation.totalFrames to true }
            },
            onEditPackItem = host.packCreatorConfig?.let { { index -> panels.packItemDetails = index to false } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun TracksWorkspace(
    state: StudioState,
    host: EditorHost,
    ui: EditorUiState,
    repositories: EditorRepositories,
    commands: EditorCommands,
) {
    val panels = ui.panels
    val closeThen: (() -> Unit) -> () -> Unit = { action ->
        {
            panels.tracksWorkspaceVisible = false
            action()
        }
    }
    TracksWorkspaceDialog(
        isPlaying = state.animation.isPlaying,
        onIntent = commands.onIntent,
        onDismiss = { panels.tracksWorkspaceVisible = false },
        assetPicker = {
            ActorPerformancePicker(
                state.project.id,
                state.project.fps,
                state.animation.currentFrame,
                repositories.actors,
                repositories.performances,
                onPlace = {
                    panels.tracksWorkspaceVisible = false
                    commands.placeActor(it)
                },
                onCreatePerformance = host.onCreatePerformance,
                onImportAudio = closeThen { commands.pickers.importAudio() },
                onRecordAudio = closeThen { commands.pickers.recordAudio() },
                onImportVideo = closeThen { commands.pickers.importVideo() },
                onFromLibrary = closeThen { panels.showLibraryPicker = true },
            )
        },
    ) {
        TimelineTracksPanel(
            workspaceMode = true,
            state = state,
            onIntent = commands.onIntent,
            performanceRepository = repositories.performances,
            onEditPlacement = { placement -> host.onEditPerformance?.invoke(placement, state.project.id) },
            onCreatePerformance = host.onCreatePerformance,
            filmstripScrollState = ui.timelineScrollState,
            onAddToLibrary = commands.exports::exportPerformance,
            onAddAudioToLibrary = commands.exports::exportAudioClip,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
