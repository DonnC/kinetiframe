package com.donnclab.kinetiframe.actors.ui

import com.donnclab.kinetiframe.actors.data.ActorManifest
import com.donnclab.kinetiframe.actors.data.ActorRevision
import com.donnclab.kinetiframe.actors.data.PerformanceDocument
import com.donnclab.kinetiframe.project.model.Layer
import com.donnclab.kinetiframe.project.model.LayerType
import com.donnclab.kinetiframe.project.model.ProjectDocument
import com.donnclab.kinetiframe.project.model.PuppetDefinition
import com.donnclab.kinetiframe.project.model.PuppetInstance
import com.donnclab.kinetiframe.studio.ActorStudioState
import com.donnclab.kinetiframe.studio.LayerState
import com.donnclab.kinetiframe.studio.SceneState
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.ToolState

/** Adapts one immutable actor revision to the existing reusable canvas/rig engine without a project document. */
fun actorStudioInitialState(
    manifest: ActorManifest,
    revision: ActorRevision,
    performance: PerformanceDocument? = null,
    performanceAuthoring: Boolean = performance != null,
): StudioState {
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
        performance?.poseInterpolation ?: com.donnclab.kinetiframe.actors.data.CurveInterpolation.SMOOTH,
        motionPlayback = performance?.playback ?: com.donnclab.kinetiframe.actors.data.PerformancePlayback.LOOP,
        artworkKeys = performance?.artworkKeys.orEmpty(),
    )
    val layer = Layer(
        name = manifest.displayName,
        id = "studio-layer-${manifest.actorId}",
        type = LayerType.PUPPET,
        puppetInstanceId = instance.id,
    )
    return StudioState(
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
        tool = ToolState(activeTool = com.donnclab.kinetiframe.project.model.DrawingTool.RIG_SELECT),
    )
}
