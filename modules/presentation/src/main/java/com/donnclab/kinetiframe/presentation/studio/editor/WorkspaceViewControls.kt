// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.res.stringResource
import com.donnclab.kinetiframe.actors.render.ActorRendering
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.studio.StudioViewModel
import com.donnclab.kinetiframe.presentation.studio.actorstudio.ActorReferenceControls
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState

/**
 * The frame reference and camera buttons for the tablet workspace's top bar. On a phone they float
 * at the canvas's corner; a tablet keeps the canvas clear and puts them with the other view actions.
 */
@Composable
internal fun WorkspaceViewControls(
    viewModel: StudioViewModel,
    state: StudioState,
    ui: EditorUiState,
    actorRendering: ActorRendering,
    onIntent: (StudioIntent) -> Unit,
) {
    val stage = state.project.stage
    if (!state.animation.isPlaying) {
        val reference = viewModel.frameReferences.frame(state.animation.currentFrame)
        key(reference) {
            ActorReferenceControls(
                reference,
                stage.width,
                stage.height,
                description = stringResource(R.string.reference_frame_description, state.animation.currentFrame + 1),
                onReplace = { image -> viewModel.frameReferences.replace(reference, image, stage.width, stage.height) },
            )
        }
    }
    CameraControls(viewModel, state, actorRendering, ui.panels.cameraPreview, onIntent)
}
