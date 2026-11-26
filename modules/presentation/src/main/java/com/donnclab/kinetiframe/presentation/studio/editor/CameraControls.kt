// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.donnclab.kinetiframe.actors.render.ActorRendering
import com.donnclab.kinetiframe.core.document.camera.CameraAction
import com.donnclab.kinetiframe.presentation.studio.StudioViewModel
import com.donnclab.kinetiframe.presentation.studio.camera.CameraWorkspaceControls
import com.donnclab.kinetiframe.presentation.studio.canvas.CanvasArea
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.media.VideoFrameProvider

/**
 * The camera button and its effects menu, with the fitted preview its shot editor shows. On a
 * phone it sits on the canvas; in the tablet workspace it sits in the top bar.
 */
@Composable
internal fun CameraControls(
    viewModel: StudioViewModel,
    state: StudioState,
    actorRendering: ActorRendering,
    cameraPreview: MutableState<Boolean>,
    onIntent: (StudioIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var preview by cameraPreview
    CameraWorkspaceControls(state, preview, { preview = it }, onIntent, modifier) { pose, shot, frame ->
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
            actorRendering = actorRendering,
            projectAssetManager = viewModel.projectAssetManager,
            videoFrameProvider = previewVideo,
        )
    }
}
