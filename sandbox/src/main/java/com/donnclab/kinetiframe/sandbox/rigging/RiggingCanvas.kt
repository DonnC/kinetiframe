// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.sandbox.rigging

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.sandbox.R

@Composable
internal fun RiggingCanvas(state: SandboxState, onIntent: (SandboxIntent) -> Unit, modifier: Modifier = Modifier) {
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val latestIntent by rememberUpdatedState(onIntent)
    val line = MaterialTheme.colorScheme.primary
    val target = MaterialTheme.colorScheme.tertiary
    val meshColor = MaterialTheme.colorScheme.secondary.copy(alpha = .5f)
    val mesh = remember(state.pose.joints, state.previewMode) {
        when (state.previewMode) {
            RigPreviewMode.BONES -> null
            RigPreviewMode.SKIN_MESH -> RigMeshFixture.deform(state.pose.joints)
        }
    }
    val description = stringResource(R.string.rigging_canvas)
    val scale = minOf(viewport.width / RigFixture.WIDTH, viewport.height / RigFixture.HEIGHT).coerceAtLeast(.001f)
    Canvas(
        modifier.onSizeChanged { viewport = it }.semantics { contentDescription = description }
            .pointerInput(scale) {
                detectDragGestures { change, _ ->
                    change.consume()
                    latestIntent(SandboxIntent.Drag(Point2(change.position.x / scale, change.position.y / scale)))
                }
            },
    ) {
        fun screen(point: Point2) = Offset(point.x * scale, point.y * scale)
        mesh?.let { vertices ->
            fun vertex(index: Int) = Offset(vertices[index * 2] * scale, vertices[index * 2 + 1] * scale)
            for (row in 0..RigMeshFixture.ROWS) {
                for (column in 0..RigMeshFixture.COLUMNS) {
                    val index = row * (RigMeshFixture.COLUMNS + 1) + column
                    if (column < RigMeshFixture.COLUMNS) drawLine(meshColor, vertex(index), vertex(index + 1), 2f)
                    if (row <
                        RigMeshFixture.ROWS
                    ) {
                        drawLine(meshColor, vertex(index), vertex(index + RigMeshFixture.COLUMNS + 1), 2f)
                    }
                }
            }
        }
        for (index in 0 until state.pose.joints.lastIndex) {
            drawLine(line, screen(state.pose.joints[index]), screen(state.pose.joints[index + 1]), 4f)
        }
        state.pose.joints.forEach { drawCircle(line, 7f, screen(it)) }
        drawCircle(target, 9f, screen(state.pose.target))
    }
}

@Preview(widthDp = 640, heightDp = 360)
@Composable
private fun RiggingCanvasPreview() {
    MaterialTheme { RiggingCanvas(SandboxState(), {}) }
}

@Preview(widthDp = 640, heightDp = 360)
@Composable
private fun RiggingMeshPreview() {
    MaterialTheme { RiggingCanvas(SandboxState(previewMode = RigPreviewMode.SKIN_MESH), {}) }
}
