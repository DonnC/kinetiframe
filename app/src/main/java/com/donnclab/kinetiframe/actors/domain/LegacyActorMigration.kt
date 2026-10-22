package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.data.ActorRevision
import com.donnclab.kinetiframe.actors.data.PerformanceDocument
import com.donnclab.kinetiframe.actors.data.PerformancePlayback
import com.donnclab.kinetiframe.actors.data.validate
import com.donnclab.kinetiframe.project.model.PuppetDefinition
import com.donnclab.kinetiframe.project.model.PuppetInstance

/** Explicit conversion boundary for the legacy in-memory Actor Studio prototype. */
object LegacyActorMigration {
    fun revision(definition: PuppetDefinition, actorId: String, revisionId: String, nowEpochMs: Long): ActorRevision =
        ActorRevision(
            revisionId = revisionId,
            actorId = actorId,
            sceneGraph = definition.sceneGraph,
            createdAtEpochMs = nowEpochMs,
        ).validate()

    fun performance(
        instance: PuppetInstance,
        projectId: String,
        actorId: String,
        revisionId: String,
        performanceId: String,
        name: String,
        nowEpochMs: Long,
    ): PerformanceDocument? {
        val preset = instance.motionPreset
        if (instance.poseKeys.isEmpty() && instance.rotationCurves.isEmpty() && instance.rootMotionCurves.isEmpty() &&
            instance.artworkKeys.isEmpty() &&
            preset == null
        ) {
            return null
        }
        val requested = maxOf(
            preset?.durationMicros ?: 1_000_000L,
            instance.poseKeys.maxOfOrNull { it.timeMicros } ?: 0L,
            instance.rotationCurves.maxOfOrNull { it.points.lastOrNull()?.timeMicros ?: 0L } ?: 0L,
            instance.rootMotionCurves.maxOfOrNull { it.points.lastOrNull()?.timeMicros ?: 0L } ?: 0L,
            instance.artworkKeys.maxOfOrNull { it.timeMicros } ?: 0L,
        )
        val duration = if (instance.artworkKeys.isEmpty()) {
            requested
        } else {
            PerformanceDuration.expressionLoop(
                requested,
                preset,
            )
        }
        return PerformanceDocument(
            performanceId = performanceId, projectId = projectId, actorId = actorId,
            actorRevisionId = revisionId, displayName = name, durationMicros = duration,
            playback = if (preset?.recipe?.loop == false) PerformancePlayback.END_HOLD else PerformancePlayback.LOOP,
            poseKeys = instance.poseKeys, presetSnapshot = preset,
            rotationCurves = instance.rotationCurves, poseInterpolation = instance.poseInterpolation,
            artworkKeys = instance.artworkKeys, rootMotionCurves = instance.rootMotionCurves,
            createdAtEpochMs = nowEpochMs, updatedAtEpochMs = nowEpochMs,
        ).validate()
    }
}
