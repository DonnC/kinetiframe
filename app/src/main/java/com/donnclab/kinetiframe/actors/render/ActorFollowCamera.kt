package com.donnclab.kinetiframe.actors.render

import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.camera.CameraPose
import com.donnclab.kinetiframe.studio.camera.SceneCamera

/** Causal, bounded sampling instead of a stateful spring: seeking backwards gives the same shot.
 * This runs during frame preparation, never in a Compose draw callback. */
object ActorFollowCamera {
    fun evaluate(project: ProjectDocument, frame: Int, renderer: ActorFrameRenderer?): CameraPose =
        SceneCamera.evaluate(project.cameraShots, frame, project.fps) center@{ shot, targetFrame ->
            val placement = project.actorPlacements[shot.targetPlacementId] ?: return@center null
            if (renderer == null) return@center null
            val last = placement.startFrame + placement.durationFrames * placement.repeatCount - 1
            val window = (shot.followSmoothingSeconds * project.fps).toInt()
            val samples = (0..4).map { (targetFrame - window * it / 4).coerceIn(placement.startFrame, last) }.distinct()
            val centers = samples.mapNotNull { sample ->
                val points = renderer.resolveAll(listOf(placement), project.id, sample, project.fps)
                    .firstOrNull()?.render?.geometry?.quads?.flatten().orEmpty()
                if (points.isEmpty()) {
                    null
                } else {
                    Pair(
                        (points.minOf { it.x } + points.maxOf { it.x }) / (2 * project.stage.width),
                        (points.minOf { it.y } + points.maxOf { it.y }) / (2 * project.stage.height),
                    )
                }
            }
            if (centers.isEmpty()) {
                null
            } else {
                Pair(
                    centers.sumOf { it.first.toDouble() }.toFloat() / centers.size,
                    centers.sumOf { it.second.toDouble() }.toFloat() / centers.size,
                )
            }
        }
}
