// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.donnclab.kinetiframe.actors.render.ActorRendering
import com.donnclab.kinetiframe.core.document.camera.CameraAction
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.LocalAppPreferences
import com.donnclab.kinetiframe.presentation.studio.StudioViewModel
import com.donnclab.kinetiframe.presentation.studio.actorstudio.ActorReferenceControls
import com.donnclab.kinetiframe.presentation.studio.camera.CameraWorkspaceControls
import com.donnclab.kinetiframe.presentation.studio.camera.CameraWorkspacePolicy
import com.donnclab.kinetiframe.presentation.studio.canvas.CanvasArea
import com.donnclab.kinetiframe.presentation.studio.dock.ConstructionShapeDock
import com.donnclab.kinetiframe.presentation.studio.dock.FillDock
import com.donnclab.kinetiframe.presentation.studio.dock.SelectionDock
import com.donnclab.kinetiframe.presentation.studio.dock.ToolDock
import com.donnclab.kinetiframe.presentation.studio.timeline.StudioFocusControls
import com.donnclab.kinetiframe.presentation.studio.workspace.usesWorkspace
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.media.VideoFrameProvider

/** The canvas and everything layered over it, back to front. */
@Composable
internal fun BoxScope.EditorCanvasStack(
    viewModel: StudioViewModel,
    state: StudioState,
    host: EditorHost,
    ui: EditorUiState,
    actorRendering: ActorRendering,
    commands: EditorCommands,
) {
    val panels = ui.panels
    val toolbarVisible = panels.toolbarVisible
    val isPuppetStudio = state.scene.studioMode != StudioMode.RASTER_DRAWING
    val showsCamera = !isPuppetStudio && !host.isPackCreator
    val cameraPreview = panels.cameraPreview
    // The tablet workspace puts the camera and frame reference buttons in the top bar instead.
    val workspace = usesWorkspace(state, host.isActorStudioSession, host.isPackCreator)
    val frameReference = if (!host.isActorStudioSession && showsCamera && !state.animation.isPlaying) {
        viewModel.frameReferences.frame(state.animation.currentFrame)
    } else {
        null
    }
    LaunchedEffect(frameReference) {
        // Returning to a frame restores its guide, not an old transform gesture or menu.
        frameReference?.editing = false
        frameReference?.menuOpen = false
    }

    CanvasArea(
        cameraPreview = showsCamera && (cameraPreview.value || state.animation.isPlaying),
        actorReference = host.actorReference ?: frameReference,
        state = state,
        onIntent = commands.onIntent,
        modifier = Modifier.fillMaxSize(),
        cacheManager = viewModel.cacheManager,
        rigAssetManager = viewModel.rigAssetManager,
        actorRendering = actorRendering,
        projectAssetManager = viewModel.projectAssetManager,
        videoFrameProvider = viewModel.videoFrameProvider,
    )
    if (showsCamera && !workspace) {
        CameraControls(
            viewModel,
            state,
            actorRendering,
            cameraPreview,
            commands.onIntent,
            // Below the frame reference button, which shares the top-right corner. They used to sit
            // on top of each other, so the camera only showed during playback.
            Modifier.align(
                Alignment.TopEnd,
            ).padding(top = Dimens.spaceS + Dimens.touchTarget + Dimens.spaceS, end = Dimens.spaceS),
        )
    }
    if (!toolbarVisible && !isPuppetStudio) {
        StudioFocusControls(
            state.animation.isPlaying,
            commands.onIntent,
            onRestore = { panels.toolbarVisible = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(Dimens.spaceS),
        )
    }
    if (host.isPackCreator) PackAlignmentCrosshair()

    val stage = state.project.stage
    frameReference?.takeIf { toolbarVisible && !workspace }?.let { reference ->
        key(reference) {
            ActorReferenceControls(
                reference,
                stage.width,
                stage.height,
                Modifier.align(Alignment.TopEnd).padding(Dimens.spaceS),
                description = stringResource(R.string.reference_frame_description, state.animation.currentFrame + 1),
                onReplace = { image -> viewModel.frameReferences.replace(reference, image, stage.width, stage.height) },
            )
        }
    }
    val authoringActor = host.isActorStudioSession && state.scene.studioMode != StudioMode.ANIMATE_IK
    if (toolbarVisible && authoringActor && host.actorReference != null) {
        ActorReferenceControls(
            host.actorReference,
            stage.width,
            stage.height,
            Modifier.align(Alignment.TopEnd).padding(Dimens.spaceS),
        )
    }
    ActorAuthoringOverlay(state, host, ui, commands)
    if (panels.showPresetLibrary) PresetLibraryOverlay(state, commands.onIntent) { panels.showPresetLibrary = false }

    // The dock sits above the canvas overlays so it stays draggable wherever the animator
    // places it, even over the timeline.
    val playbackHidesTools = LocalAppPreferences.current.hideToolsDuringPlayback
    // The tablet workspace's tool panel replaces the floating dock.
    if (!workspace && toolbarVisible && (!state.animation.isPlaying || !playbackHidesTools)) {
        ToolDock(
            state = state,
            onIntent = { intent ->
                if (CameraWorkspacePolicy.selectsArtwork(intent)) {
                    cameraPreview.value = false
                    if (state.animation.isPlaying) commands.onIntent(StudioIntent.Playback.Pause)
                }
                commands.onIntent(intent)
            },
            onInteraction = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
    // The floating dock carries the Confirm bars for a fill or shape preview; the workspace has no
    // dock, so it shows them here, or a shape could never be confirmed (tablet expert review).
    if (workspace && toolbarVisible) {
        if (state.fill.previewActive) FillDock(state = state, onIntent = commands.onIntent)
        if (state.constructionShape.previewActive) {
            ConstructionShapeDock(state = state, onIntent = commands.onIntent)
        }
    }
    if (toolbarVisible && !isPuppetStudio && state.selection.active && !state.animation.isPlaying) {
        SelectionDock(state = state, onIntent = commands.onIntent, modifier = Modifier.fillMaxSize())
    }
    PackTrayOverlay(ui.tray, state, toolbarVisible, commands.onIntent)
}

/**
 * A faint crosshair so a pack's drawings line up across frames, such as an angry eye's pupil
 * sitting where a winking eye's does. It is never drawn into the frame and never takes touches.
 */
@Composable
private fun PackAlignmentCrosshair() {
    Canvas(Modifier.fillMaxSize()) {
        val strokeColor = Color.Black.copy(alpha = 0.2f)
        drawLine(strokeColor, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), strokeWidth = 2f)
        drawLine(strokeColor, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), strokeWidth = 2f)
    }
}
