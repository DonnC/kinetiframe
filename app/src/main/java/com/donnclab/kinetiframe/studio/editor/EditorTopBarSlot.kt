// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

import androidx.compose.runtime.Composable
import com.donnclab.kinetiframe.settings.LocalAppPreferences
import com.donnclab.kinetiframe.studio.PerformanceTakePhase
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.activeActor
import com.donnclab.kinetiframe.studio.actorPreviewDuration
import com.donnclab.kinetiframe.studio.ui.EditorTopBar
import com.donnclab.kinetiframe.studio.ui.actorstudio.rigging.PuppetTopBar

/** The rigging and posing bar in Actor Studio's puppet modes, and the drawing bar everywhere else. */
@Composable
internal fun EditorTopBarSlot(state: StudioState, host: EditorHost, ui: EditorUiState, commands: EditorCommands) {
    val panels = ui.panels
    val onIntent = commands.onIntent
    val mode = state.scene.studioMode
    val chooseArtwork: (Boolean) -> Unit = { asVariant -> ui.artwork.choose(asVariant, state, onIntent) }
    if (mode != StudioMode.RASTER_DRAWING) {
        PuppetTopBar(
            onIntent,
            onInspectorToggle = {
                if (mode == StudioMode.RIG_SETUP) panels.showPartPicker = true else panels.showPresetLibrary = true
            },
            studioMode = mode,
            skeletonOverlayVisible = state.scene.skeletonOverlayVisible,
            performanceOnly = state.actor.isPerformanceAuthoring,
            canUndo = state.actor.canUndo,
            canRedo = state.actor.canRedo,
            onSave = commands.actorSaves.saveActor?.takeIf { mode == StudioMode.RIG_SETUP },
            onSavePerformance = host.onSavePerformance
                ?.takeIf { mode == StudioMode.ANIMATE_IK && state.actor.takePhase == PerformanceTakePhase.IDLE }
                ?.let { save ->
                    { state.activeActor()?.let { instance -> save(instance, state.actorPreviewDuration()) } }
                },
            onPerformanceActions = host.onPerformanceActions?.takeIf { mode == StudioMode.ANIMATE_IK },
            onExit = host.onExitActorStudio?.let { exit -> { commands.requestExit(exit) } },
            actorViews = host.actorViews,
            onAttachArtwork = chooseArtwork.takeIf { host.isActorStudioSession },
            hasSelectedPart = state.scene.selectedBoneId != null,
        )
        return
    }
    val playbackHidesTools = LocalAppPreferences.current.hideToolsDuringPlayback
    if (!panels.toolbarVisible || (state.animation.isPlaying && playbackHidesTools)) return
    val isActorStudio = host.isActorStudioSession
    EditorTopBar(
        onFocusCanvas = { panels.toolbarVisible = false },
        onTracksOpen = if (!host.isPackCreator && !isActorStudio) ({ panels.tracksWorkspaceVisible = true }) else null,
        onEditPackItem = host.packCreatorConfig?.let {
            { panels.packItemDetails = state.animation.currentFrame to false }
        },
        state = state,
        onIntent = onIntent,
        actorViews = host.actorViews,
        onAttachActorArtwork = chooseArtwork.takeIf { isActorStudio && !state.actor.isPerformanceAuthoring },
        onHomeClick = (host.onHomeClick ?: host.onExitActorStudio)?.let { home -> { commands.requestExit(home) } },
        onStudioClick = { panels.frameStudioVisible = true },
        onGridSettingsClick = { panels.showGridMenu = true },
        onOnionSettingsClick = { panels.showOnionMenu = true },
        // In a project's editor this only toggles Rig Setup in place. Actor Studio is its own
        // destination, so a project never navigates there from here.
        isActorStudio = isActorStudio,
        onSaveActorClick = commands.actorSaves.saveActor,
        showOverflowMenu = panels.showOverflowMenu,
        onOverflowMenuToggle = { panels.showOverflowMenu = it },
        onSetBackgroundClick = { commands.pickers.pickBackground() },
        onImportFrameImage = if (!isActorStudio) ({ commands.pickers.pickFrameImage() }) else null,
        onSaveProjectClick = commands.saves.saveProject,
        onAddBackgroundToLibraryClick = { commands.exports.exportBackground() },
        onAddCurrentFrameToLibraryClick = { commands.exports.exportCurrentFrame() },
        // Pack Creator reads frames back through its own FrameCacheManager, and a frame drawn in
        // this session can still be in memory only. Preparing the save writes it to disk first.
        onSavePackClick = host.packCreatorConfig?.onSavePack?.let { { commands.saves.savePack() } },
    )
}
