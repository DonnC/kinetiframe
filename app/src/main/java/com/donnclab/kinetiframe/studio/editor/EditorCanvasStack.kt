// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

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
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.actors.ui.ActorReferenceControls
import com.donnclab.kinetiframe.core.document.camera.CameraAction
import com.donnclab.kinetiframe.settings.LocalAppPreferences
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.StudioViewModel
import com.donnclab.kinetiframe.studio.ui.camera.CameraWorkspaceControls
import com.donnclab.kinetiframe.studio.ui.camera.CameraWorkspacePolicy
import com.donnclab.kinetiframe.studio.ui.canvas.CanvasArea
import com.donnclab.kinetiframe.studio.ui.dock.SelectionDock
import com.donnclab.kinetiframe.studio.ui.dock.ToolDock
import com.donnclab.kinetiframe.studio.ui.timeline.StudioFocusControls
import com.donnclab.kinetiframe.video.VideoFrameProvider

/** The canvas and everything layered over it, back to front. */
@Composable
internal fun BoxScope.EditorCanvasStack(
    viewModel: StudioViewModel,
    state: StudioState,
    host: EditorHost,
    ui: EditorUiState,
    repositories: EditorRepositories,
    commands: EditorCommands,
) {
    val panels = ui.panels
    val toolbarVisible = panels.toolbarVisible
    val isPuppetStudio = state.scene.studioMode != StudioMode.RASTER_DRAWING
    val showsCamera = !isPuppetStudio && !host.isPackCreator
    val cameraPreview = remember(state.project.id) { mutableStateOf(CameraWorkspacePolicy.PREVIEW_ON_OPEN) }
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
        actorFiles = repositories.actorStore,
        actorRepository = repositories.actors,
        performanceRepository = repositories.performances,
        projectAssetManager = viewModel.projectAssetManager,
        videoFrameProvider = viewModel.videoFrameProvider,
    )
    if (showsCamera) CameraOverlay(viewModel, state, repositories, cameraPreview, commands.onIntent)
    if (!toolbarVisible && !isPuppetStudio) {
        StudioFocusControls(
            state.animation.isPlaying,
            commands.onIntent,
            onRestore = { panels.toolbarVisible = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
        )
    }
    if (host.isPackCreator) PackAlignmentCrosshair()

    val stage = state.project.stage
    frameReference?.takeIf { toolbarVisible }?.let { reference ->
        key(reference) {
            ActorReferenceControls(
                reference,
                stage.width,
                stage.height,
                Modifier.align(Alignment.TopEnd).padding(8.dp),
                description = "Reference for frame ${state.animation.currentFrame + 1} (temporary)",
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
            Modifier.align(Alignment.TopEnd).padding(8.dp),
        )
    }
    ActorAuthoringOverlay(state, host, ui, commands)
    if (panels.showPresetLibrary) PresetLibraryOverlay(state, commands.onIntent) { panels.showPresetLibrary = false }

    // The dock sits above the canvas overlays so it stays draggable wherever the animator
    // places it, even over the timeline.
    val playbackHidesTools = LocalAppPreferences.current.hideToolsDuringPlayback
    if (toolbarVisible && (!state.animation.isPlaying || !playbackHidesTools)) {
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
    if (toolbarVisible && !isPuppetStudio && state.selection.active && !state.animation.isPlaying) {
        SelectionDock(state = state, onIntent = commands.onIntent, modifier = Modifier.fillMaxSize())
    }
    PackTrayOverlay(ui.tray, state, toolbarVisible, commands.onIntent)
}

@Composable
private fun BoxScope.CameraOverlay(
    viewModel: StudioViewModel,
    state: StudioState,
    repositories: EditorRepositories,
    cameraPreview: MutableState<Boolean>,
    onIntent: (StudioIntent) -> Unit,
) {
    var preview by cameraPreview
    CameraWorkspaceControls(
        state,
        preview,
        { preview = it },
        onIntent,
        Modifier.align(Alignment.TopEnd).padding(8.dp),
    ) { pose, shot, frame ->
        val previewState = if (shot == null || frame == null) {
            state
        } else {
            state.copy(
                project = state.project.copy(
                    cameraShots =
                    state.project.cameraShots.filter { it.id != shot.id && it.endFrame <= shot.startFrame } +
                        shot,
                ),
                animation = state.animation.copy(currentFrame = frame, playbackFrame = frame, isPlaying = true),
            )
        }
        val latestPreview by rememberUpdatedState(previewState)
        // The dialog owns its decoder. Closing it must not leave the real
        // workspace holding a decoded video frame from the preview clock.
        val previewVideo = remember { VideoFrameProvider { latestPreview } }
        DisposableEffect(previewVideo) { onDispose { previewVideo.release() } }
        CanvasArea(
            state = previewState,
            onIntent = {},
            cameraPreview = true,
            cameraPoseOverride = if (shot?.action == CameraAction.FOLLOW) null else pose,
            fitStage = true,
            cacheManager = viewModel.cacheManager,
            actorFiles = repositories.actorStore,
            actorRepository = repositories.actors,
            performanceRepository = repositories.performances,
            projectAssetManager = viewModel.projectAssetManager,
            videoFrameProvider = previewVideo,
        )
    }
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
