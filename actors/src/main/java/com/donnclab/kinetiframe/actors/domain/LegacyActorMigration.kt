package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.rig.presets.AppliedActorPreset
import com.donnclab.kinetiframe.core.actor.document.ActorRevision
import com.donnclab.kinetiframe.core.actor.document.PerformanceDocument
import com.donnclab.kinetiframe.core.actor.document.validate
import com.donnclab.kinetiframe.core.actor.motion.PerformancePlayback
import com.donnclab.kinetiframe.core.document.PuppetDefinition
import com.donnclab.kinetiframe.core.document.PuppetInstance

/** Explicit conversion boundary for the legacy in-memory Actor Studio prototype. */
object LegacyActorMigration {
    fun revision(definition: PuppetDefinition, actorId: String, revisionId: String, nowEpochMs: Long): ActorRevision =
        ActorRevision(
            revisionId = revisionId,
            actorId = actorId,
            sceneGraph = definition.sceneGraph,
            createdAtEpochMs = nowEpochMs,
        ).validate()

    fun performance(instance: PuppetInstance, identity: PerformanceIdentity, nowEpochMs: Long): PerformanceDocument? {
        val (projectId, actorId, revisionId, performanceId, name) = identity
        val preset = instance.motionPreset?.let(AppliedActorPreset::of)
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
            poseKeys = instance.poseKeys, presetSnapshot = preset?.document(),
            rotationCurves = instance.rotationCurves, poseInterpolation = instance.poseInterpolation,
            artworkKeys = instance.artworkKeys, rootMotionCurves = instance.rootMotionCurves,
            createdAtEpochMs = nowEpochMs, updatedAtEpochMs = nowEpochMs,
        ).validate()
    }
}

/** Who a converted performance belongs to, and its ID and display name. */
data class PerformanceIdentity(
    val projectId: String,
    val actorId: String,
    val revisionId: String,
    val performanceId: String,
    val name: String,
)
