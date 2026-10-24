package com.donnclab.kinetiframe.studio.ui.actorstudio.rigging

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.actors.render.ActorFrameRenderer
import com.donnclab.kinetiframe.core.document.ActorPlacementReference
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.studio.render.completedPreviewFrames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withContext

/**
 * Thin Compose wrapper around [ActorFrameRenderer] -- resolving actor placements and drawing them
 * both now live in that plain, non-Compose class so project export and thumbnail generation share
 * the exact same code path as this live canvas rather than a second implementation that could
 * drift from it. This composable owns only the renderer's lifecycle and where its output lands on
 * screen (the live pan/zoom [canvasMatrix], unlike export's identity viewport).
 */
@Composable
fun ProjectActorRenderLayer(
    placements: Collection<ActorPlacementReference>,
    projectId: String,
    frame: Int,
    fps: Int,
    canvasMatrix: FloatArray,
    actors: ActorRepository,
    performances: PerformanceRepository,
    files: DocumentFileStore,
    modifier: Modifier = Modifier,
    onGeometryResolved: (com.donnclab.kinetiframe.actors.render.ActorSelectionGeometry) -> Unit = {},
    selectedIds: Set<String> = emptySet(),
    selectionPreview: (() -> androidx.compose.ui.graphics.Matrix)? = null,
) {
    val renderer = remember(actors, performances, files) { ActorFrameRenderer(actors, performances, files) }
    DisposableEffect(renderer) { onDispose { renderer.clear() } }
    var renders by remember { mutableStateOf<List<ActorFrameRenderer.Render>>(emptyList()) }
    var diagnostics by remember { mutableStateOf<List<ActorFrameRenderer.Diagnostic>>(emptyList()) }
    // File/repository resolution happens off the main thread. Keep the last completed pose
    // visible; a slow request may finish after the clock advances without blocking that clock.
    val latestFrame by rememberUpdatedState(frame)
    val latestFps by rememberUpdatedState(fps)
    val geometryCallback by rememberUpdatedState(onGeometryResolved)
    LaunchedEffect(renderer, placements.toList(), projectId) {
        // Finish the in-flight pose, then take the newest pending frame. Cancelling on every
        // tick can starve all visible updates when a cold document takes longer than one tick.
        snapshotFlow { latestFrame to latestFps }.completedPreviewFrames { (requestedFrame, requestedFps) ->
            withContext(Dispatchers.IO) { renderer.resolveAll(placements, projectId, requestedFrame, requestedFps) }
        }.collect { (request, resolution) ->
            val requestedFrame = request.first
            renders = resolution.mapNotNull(ActorFrameRenderer.Resolution::render)
            geometryCallback(
                com.donnclab.kinetiframe.actors.render.ActorSelectionGeometry(
                    projectId,
                    requestedFrame,
                    renders.map {
                        it.geometry
                    },
                ),
            )
            diagnostics = resolution.mapNotNull(ActorFrameRenderer.Resolution::diagnostic)
        }
    }
    val viewport = remember(canvasMatrix) { android.graphics.Matrix().apply { setValues(canvasMatrix) } }
    val previewMatrix = remember { android.graphics.Matrix() }
    val previewValues = remember { FloatArray(9) }
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                val preview = selectionPreview?.invoke()?.let { matrix ->
                    val v = matrix.values
                    previewValues[0] = v[0]
                    previewValues[1] = v[4]
                    previewValues[2] = v[12]
                    previewValues[3] = v[1]
                    previewValues[4] = v[5]
                    previewValues[5] = v[13]
                    previewValues[6] = 0f
                    previewValues[7] = 0f
                    previewValues[8] = 1f
                    previewMatrix.apply { setValues(previewValues) }
                }
                renderer.draw(canvas.nativeCanvas, viewport, renders, selectedIds, preview)
            }
        }
        if (diagnostics.isNotEmpty()) {
            ProjectActorRenderWarning(
                diagnostics,
                Modifier.align(Alignment.TopStart).padding(12.dp),
            )
        }
    }
}

@Composable
private fun ProjectActorRenderWarning(diagnostics: List<ActorFrameRenderer.Diagnostic>, modifier: Modifier = Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
        Text(
            text = if (diagnostics.size ==
                1
            ) {
                diagnostics.first().message
            } else {
                "${diagnostics.size} actor placements need attention"
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
