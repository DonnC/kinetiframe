package com.donnclab.kinetiframe.presentation.studio.actorstudio.rigging

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.actors.render.toMatrix
import com.donnclab.kinetiframe.actors.rig.math.CutoutPartGuideFactory
import com.donnclab.kinetiframe.actors.rig.math.ForwardKinematicsSolver
import com.donnclab.kinetiframe.studio.ActorEditTarget
import com.donnclab.kinetiframe.studio.PerformanceTakePhase
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.activeActor
import com.donnclab.kinetiframe.studio.actorDefinition

private data class RigSegment(val id: String, val start: Offset, val end: Offset, val bound: Boolean)

/** Constant screen-size endpoint handles remain usable at every viewport zoom. */
@Composable
fun RigSkeletonOverlay(state: StudioState, modifier: Modifier = Modifier) {
    val definition = state.actorDefinition(true)
    val instance = state.activeActor()
    val segments = remember(definition, instance, state.canvas.matrixValues) {
        if (definition == null || instance == null) {
            emptyList()
        } else {
            val matrix = instance.transform.toMatrix().apply {
                postConcat(android.graphics.Matrix().apply { setValues(state.canvas.matrixValues) })
            }
            ForwardKinematicsSolver.resolveWorldStates(definition.sceneGraph).values.map { bone ->
                val points = floatArrayOf(bone.startPoint.x, bone.startPoint.y, bone.endPoint.x, bone.endPoint.y)
                matrix.mapPoints(points)
                val boneNode = definition.sceneGraph.bones[bone.id]
                RigSegment(
                    bone.id,
                    Offset(points[0], points[1]),
                    Offset(points[2], points[3]),
                    bound = boneNode?.textureBinding != null,
                )
            }
        }
    }
    val accent = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.surface
    // In Setup, an unbound part's joint reads as a hollow warning dot rather than the filled
    // accent circle -- the same "still needs artwork" signal as the tree's Link2Off icon, but
    // visible right where the user is sketching instead of only in the side panel.
    val needsArtwork = MaterialTheme.colorScheme.error
    val movingCharacter =
        state.actor.takeMoveCharacter &&
            state.actor.takePhase in listOf(PerformanceTakePhase.ARMED, PerformanceTakePhase.RECORDING)
    Canvas(modifier.fillMaxSize()) {
        segments.forEach { segment ->
            if (movingCharacter) {
                if (segment.id in definition?.sceneGraph?.rootBoneIds.orEmpty()) {
                    val p = segment.start
                    val radius = 12.dp.toPx()
                    drawCircle(fill, radius + 3.dp.toPx(), p)
                    drawCircle(accent, radius + 3.dp.toPx(), p, style = Stroke(2.dp.toPx()))
                    drawLine(accent, p - Offset(radius, 0f), p + Offset(radius, 0f), 2.dp.toPx())
                    drawLine(accent, p - Offset(0f, radius), p + Offset(0f, radius), 2.dp.toPx())
                }
                return@forEach
            }
            val unresolved = state.scene.studioMode == StudioMode.RIG_SETUP && !segment.bound
            val jointColor = if (unresolved) needsArtwork else accent
            val selected = segment.id == state.scene.selectedBoneId
            if (selected && state.scene.studioMode == StudioMode.RIG_SETUP &&
                state.actor.editTarget == ActorEditTarget.JOINT_START
            ) {
                drawCircle(fill, 8.dp.toPx(), segment.start)
                drawCircle(accent, 8.dp.toPx(), segment.start, style = Stroke(2.dp.toPx()))
            }
            if (selected) {
                // This is a cut-out boundary, not another deforming bone: it is generated from
                // the same two freely draggable joint nodes the renderer uses. The capsule says
                // exactly where this part starts/ends; the end halos show the intentional overlap
                // allowance needed for a seamless elbow, knee, shoulder, or hip.
                val guide = CutoutPartGuideFactory.create(segment.id, segment.start, segment.end)
                drawLine(accent.copy(alpha = 0.11f), guide.start, guide.end, guide.envelopeRadius * 2f)
                drawLine(accent.copy(alpha = 0.78f), guide.start, guide.end, 2.dp.toPx())
                drawCircle(accent.copy(alpha = 0.12f), guide.overlapRadius, guide.start)
                drawCircle(accent.copy(alpha = 0.18f), guide.overlapRadius, guide.end)
                drawCircle(accent.copy(alpha = 0.44f), guide.overlapRadius * 0.58f, guide.start)
                drawCircle(accent.copy(alpha = 0.56f), guide.overlapRadius * 0.58f, guide.end)
            }
            drawLine(jointColor.copy(alpha = 0.7f), segment.start, segment.end, 2.dp.toPx())
            drawCircle(fill, 6.dp.toPx(), segment.end)
            drawCircle(jointColor, (if (selected) 9 else 6).dp.toPx(), segment.end, style = Stroke(2.dp.toPx()))
            if (state.scene.studioMode != StudioMode.RASTER_DRAWING &&
                segment.id in definition?.sceneGraph?.rootBoneIds.orEmpty()
            ) {
                drawCircle(fill, 8.dp.toPx(), segment.start)
                drawCircle(jointColor, 8.dp.toPx(), segment.start, style = Stroke(3.dp.toPx()))
            }
        }
    }
}

@Preview(widthDp = 640, heightDp = 360)
@Composable
private fun RigSkeletonOverlayPreview() {
    RigSkeletonOverlay(StudioState())
}
