package com.donnclab.kinetiframe.ui.components.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.donnclab.kinetiframe.data.actor.ActorFileStore
import com.donnclab.kinetiframe.data.actor.ActorRepository
import com.donnclab.kinetiframe.data.actor.PerformanceRepository
import com.donnclab.kinetiframe.domain.render.ActorFollowCamera
import com.donnclab.kinetiframe.domain.render.ActorFrameRenderer
import com.donnclab.kinetiframe.domain.render.completedPreviewFrames
import com.donnclab.kinetiframe.engine.camera.CameraAction
import com.donnclab.kinetiframe.engine.camera.CameraPose
import com.donnclab.kinetiframe.engine.model.ProjectDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun followCameraPose(
    project: ProjectDocument,
    frame: Int,
    actors: ActorRepository?,
    performances: PerformanceRepository?,
    files: ActorFileStore?,
    enabled: Boolean = true,
): CameraPose? {
    val follow = project.cameraShots.any {
        it.action == CameraAction.FOLLOW && frame in it.startFrame until it.endFrame
    }
    if (!enabled || !follow || actors == null || performances == null || files == null) return null
    val renderer = remember(actors, performances, files) { ActorFrameRenderer(actors, performances, files) }
    var resolved by remember { mutableStateOf<Pair<Pair<ProjectDocument, Int>, CameraPose>?>(null) }
    val latest by rememberUpdatedState(project to frame)
    LaunchedEffect(renderer) {
        snapshotFlow { latest }.completedPreviewFrames { request ->
            withContext(Dispatchers.IO) {
                ActorFollowCamera.evaluate(request.first, request.second, renderer)
            }
        }.collect { resolved = it }
    }
    DisposableEffect(renderer) { onDispose { renderer.clear() } }
    // Retain the last completed pose while the next frame is prepared, like the actor layer.
    return resolved?.takeIf { it.first.first == project }?.second
}
