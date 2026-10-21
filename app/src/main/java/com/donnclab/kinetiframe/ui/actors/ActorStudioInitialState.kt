package com.donnclab.kinetiframe.ui.actors

import com.donnclab.kinetiframe.data.actor.ActorManifest
import com.donnclab.kinetiframe.data.actor.ActorRevision
import com.donnclab.kinetiframe.data.actor.PerformanceDocument
import com.donnclab.kinetiframe.engine.model.ProjectDocument
import com.donnclab.kinetiframe.engine.model.PuppetDefinition
import com.donnclab.kinetiframe.engine.model.PuppetInstance
import com.donnclab.kinetiframe.models.Layer
import com.donnclab.kinetiframe.models.LayerType
import com.donnclab.kinetiframe.ui.ActorStudioState
import com.donnclab.kinetiframe.ui.LayerState
import com.donnclab.kinetiframe.ui.SceneState
import com.donnclab.kinetiframe.ui.StudioMode
import com.donnclab.kinetiframe.ui.TimelineState
import com.donnclab.kinetiframe.ui.ToolState

/** Adapts one immutable actor revision to the existing reusable canvas/rig engine without a project document. */
fun actorStudioInitialState(
    manifest: ActorManifest,
    revision: ActorRevision,
    performance: PerformanceDocument? = null,
    performanceAuthoring: Boolean = performance != null,
): TimelineState {
    // A performance session is deliberately a different document role from actor setup. It loads
    // the pinned revision as an immutable puppet skin and records only motion against its IDs.
    val instance = PuppetInstance(
        id = "studio-instance-${manifest.actorId}",
        definitionId = manifest.actorId,
        poseKeys = performance?.poseKeys.orEmpty(),
        motionPreset = performance?.presetSnapshot,
        rotationCurves = performance?.rotationCurves.orEmpty(),
        rootMotionCurves = performance?.rootMotionCurves.orEmpty(),
        poseInterpolation =
        performance?.poseInterpolation ?: com.donnclab.kinetiframe.data.actor.CurveInterpolation.SMOOTH,
        motionPlayback = performance?.playback ?: com.donnclab.kinetiframe.data.actor.PerformancePlayback.LOOP,
        artworkKeys = performance?.artworkKeys.orEmpty(),
    )
    val layer = Layer(
        name = manifest.displayName,
        id = "studio-layer-${manifest.actorId}",
        type = LayerType.PUPPET,
        puppetInstanceId = instance.id,
    )
    return TimelineState(
        project = ProjectDocument(
            layers = listOf(layer),
            puppetDefinitions = mapOf(
                manifest.actorId to
                    PuppetDefinition(manifest.actorId, manifest.displayName, revision.sceneGraph),
            ),
            puppetInstances = mapOf(instance.id to instance),
        ),
        layers = LayerState(layers = listOf(layer), frameLayers = mapOf(0 to listOf(layer))),
        actor = ActorStudioState(
            isPerformanceAuthoring = performanceAuthoring,
            authoredView = revision.view,
            performanceDurationMicros = performance?.durationMicros ?: 2_000_000L,
        ),
        scene = SceneState(
            studioMode = if (performanceAuthoring) StudioMode.ANIMATE_IK else StudioMode.RIG_SETUP,
            selectedBoneId = revision.sceneGraph.rootBoneIds.firstOrNull(),
        ),
        tool = ToolState(activeTool = com.donnclab.kinetiframe.engine.model.DrawingTool.RIG_SELECT),
    )
}
