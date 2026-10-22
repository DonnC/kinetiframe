package com.donnclab.kinetiframe.actors.rig.math

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.actors.rig.model.SceneGraph

/**
 * Routes one freehand stroke to the skeletal segment it most closely follows.
 *
 * Actor artwork is captured one segment at a time so the performance renderer never has to guess
 * how to split a full-body bitmap between independently moving limbs. Averaging point-to-segment
 * distance keeps a deliberate limb stroke stable even when its endpoint reaches a joint.
 */
object RigStrokeBoneMatcher {
    fun nearestBoneId(graph: SceneGraph, points: List<Offset>): String? {
        val samples = points.filter { it.x.isFinite() && it.y.isFinite() }
        if (samples.isEmpty()) return null
        val world = ForwardKinematicsSolver.resolveWorldStates(graph)
        return world.minByOrNull { (_, bone) ->
            samples.sumOf { sample -> distanceSquaredToSegment(sample, bone.startPoint, bone.endPoint).toDouble() }
        }?.key
    }

    private fun distanceSquaredToSegment(point: Offset, start: Offset, end: Offset): Float {
        val segment = end - start
        val lengthSquared = segment.x * segment.x + segment.y * segment.y
        if (lengthSquared <= 0.0001f) return (point - start).getDistanceSquared()
        val projected = ((point - start).x * segment.x + (point - start).y * segment.y) / lengthSquared
        val nearest = start + segment * projected.coerceIn(0f, 1f)
        return (point - nearest).getDistanceSquared()
    }
}
