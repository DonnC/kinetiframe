package com.donnclab.kinetiframe.studio.ui.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.actors.render.ActorFollowCamera
import com.donnclab.kinetiframe.actors.render.ActorFrameRenderer
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.camera.CameraAction
import com.donnclab.kinetiframe.core.document.camera.CameraPose
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.studio.render.completedPreviewFrames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun followCameraPose(
    project: ProjectDocument,
    frame: Int,
    actors: ActorRepository?,
    performances: PerformanceRepository?,
    files: DocumentFileStore?,
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
