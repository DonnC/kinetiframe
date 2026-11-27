// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.presentation.studio.StudioViewModel
import com.donnclab.kinetiframe.presentation.studio.timeline.ActorPerformancePicker
import com.donnclab.kinetiframe.presentation.studio.timeline.LocalTimelineScale
import com.donnclab.kinetiframe.presentation.studio.timeline.TimelineControls
import com.donnclab.kinetiframe.presentation.studio.timeline.TimelineScale
import com.donnclab.kinetiframe.presentation.studio.timeline.TimelineServices
import com.donnclab.kinetiframe.presentation.studio.timeline.TimelineTracksPanel
import com.donnclab.kinetiframe.presentation.studio.timeline.TracksWorkspaceDialog
import com.donnclab.kinetiframe.presentation.studio.workspace.usesWorkspace
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState

/** The timeline under the canvas, and the tracks workspace it opens. Hidden in puppet modes and focus mode. */
@Composable
internal fun EditorBottomBarSlot(
    viewModel: StudioViewModel,
    state: StudioState,
    host: EditorHost,
    ui: EditorUiState,
    storage: EditorStorageViewModel,
    commands: EditorCommands,
) {
    val panels = ui.panels
    if (state.scene.studioMode != StudioMode.RASTER_DRAWING || !panels.toolbarVisible) return
    val workspace = usesWorkspace(state, host.isActorStudioSession, host.isPackCreator)
    // The workspace's strip shows frames you can read, shaped like the canvas; the track lanes
    // read the same scale, so clips stay over the frames they cover.
    val scale = if (workspace) TimelineScale.wide(state.project.stage) else TimelineScale.Phone
    CompositionLocalProvider(LocalTimelineScale provides scale) {
        BottomBarColumn(viewModel, state, host, ui, storage, commands)
    }
}

@Composable
private fun BottomBarColumn(
    viewModel: StudioViewModel,
    state: StudioState,
    host: EditorHost,
    ui: EditorUiState,
    storage: EditorStorageViewModel,
    commands: EditorCommands,
) {
    val panels = ui.panels
    val isPackCreator = host.isPackCreator
    val workspace = usesWorkspace(state, host.isActorStudioSession, isPackCreator)
    // The Scaffold's window insets are zeroed so the canvas stays edge to edge, so nothing
    // reacts to the keyboard by default. Pack Creator's name and description fields live in
    // this bar and need their own IME padding.
    Column(Modifier.fillMaxWidth().imePadding()) {
        // Pack Creator drafts are raster drawing only: no actor, audio or video tracks. The
        // workspace docks them under the canvas instead (see [WorkspaceDockedTracks]).
        if (!isPackCreator && !workspace && panels.tracksWorkspaceVisible) {
            TracksWorkspace(state, host, ui, storage.timeline, commands, docked = false)
        }
        TimelineControls(
            actorRendererFactory = storage.actorRendering::newRenderer,
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
            onLayerPanelToggle = {
                if (workspace) {
                    panels.workspaceLayersOpen = !panels.workspaceLayersOpen
                } else {
                    panels.layerSheetVisible = !panels.layerSheetVisible
                }
            },
            // Shared with the track lanes so scrolling either one keeps the other in step.
            listState = ui.timelineScrollState,
            packMode = isPackCreator,
            onAddPackItem = host.packCreatorConfig?.let {
                { panels.packItemDetails = state.animation.totalFrames to true }
            },
            onEditPackItem = host.packCreatorConfig?.let { { index -> panels.packItemDetails = index to false } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spaceXs, vertical = Dimens.spaceXs),
        )
    }
}

/**
 * The tablet workspace's asset tracks, docked under the canvas between the tool and layer panels,
 * so both panels keep their full height and the tools never scroll (tablet feedback).
 */
@Composable
internal fun WorkspaceDockedTracks(
    state: StudioState,
    host: EditorHost,
    ui: EditorUiState,
    storage: EditorStorageViewModel,
    commands: EditorCommands,
) {
    if (host.isPackCreator || !ui.panels.toolbarVisible || !ui.panels.tracksWorkspaceVisible) return
    CompositionLocalProvider(LocalTimelineScale provides TimelineScale.wide(state.project.stage)) {
        TracksWorkspace(state, host, ui, storage.timeline, commands, docked = true)
    }
}

@Composable
private fun TracksWorkspace(
    state: StudioState,
    host: EditorHost,
    ui: EditorUiState,
    timeline: TimelineServices,
    commands: EditorCommands,
    /** The tablet workspace docks tracks under the canvas instead of opening a sheet. */
    docked: Boolean,
) {
    val panels = ui.panels
    val closeThen: (() -> Unit) -> () -> Unit = { action ->
        {
            panels.tracksWorkspaceVisible = false
            action()
        }
    }
    TracksWorkspaceDialog(
        docked = docked,
        isPlaying = state.animation.isPlaying,
        onIntent = commands.onIntent,
        onDismiss = { panels.tracksWorkspaceVisible = false },
        assetPicker = {
            ActorPerformancePicker(
                state.project.id,
                state.project.fps,
                state.animation.currentFrame,
                timeline.performances,
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
            timeline = timeline,
            onEditPlacement = { placement -> host.onEditPerformance?.invoke(placement, state.project.id) },
            onCreatePerformance = host.onCreatePerformance,
            filmstripScrollState = ui.timelineScrollState,
            onAddToLibrary = commands.exports::exportPerformance,
            onAddAudioToLibrary = commands.exports::exportAudioClip,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
