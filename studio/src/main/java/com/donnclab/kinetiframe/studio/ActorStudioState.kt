package com.donnclab.kinetiframe.studio

import com.donnclab.kinetiframe.actors.domain.PerformanceDuration
import com.donnclab.kinetiframe.actors.domain.PerformanceEvaluator
import com.donnclab.kinetiframe.actors.domain.RootMotionEvaluator
import com.donnclab.kinetiframe.actors.domain.engineInterpolation
import com.donnclab.kinetiframe.actors.rig.math.ActorMotionPose
import com.donnclab.kinetiframe.actors.rig.model.ActorArtwork
import com.donnclab.kinetiframe.actors.rig.model.ActorPoseTrack
import com.donnclab.kinetiframe.actors.rig.presets.ActorMotionPreset
import com.donnclab.kinetiframe.actors.rig.presets.AppliedActorPreset
import com.donnclab.kinetiframe.core.actor.motion.PerformancePlayback
import com.donnclab.kinetiframe.core.actor.rigging.ActorView
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.PuppetDefinition
import com.donnclab.kinetiframe.core.document.PuppetInstance
import com.donnclab.kinetiframe.engine.rigging.motion.BeforeTake
import com.donnclab.kinetiframe.engine.rigging.motion.CurveSampling
import com.donnclab.kinetiframe.engine.rigging.motion.MotionComposition
import com.donnclab.kinetiframe.engine.rigging.motion.PoseSampling

/** Transient authoring state. A gesture preview never changes the saved rest pose. */
enum class ActorEditTarget { JOINT_END, JOINT_START, ARTWORK }
enum class PerformanceTakePhase { IDLE, ARMED, RECORDING, REVIEW }

data class ActorStudioState(
    val takePhase: PerformanceTakePhase = PerformanceTakePhase.IDLE,
    val takeStartMicros: Long = 0L,
    val takeEndMicros: Long = 0L,
    val takeMoveCharacter: Boolean = false,
    val takeSmoothness: Float = .35f,
    val editTarget: ActorEditTarget = ActorEditTarget.JOINT_END,
    /** A project-launched session authors motion against a frozen actor revision, never artwork. */
    val isPerformanceAuthoring: Boolean = false,
    val authoredView: ActorView = ActorView.ORIGINAL,
    /** Authoring duration is independent from display FPS and persists with the saved gesture. */
    val performanceDurationMicros: Long = 2_000_000L,
    val returnLayerId: String? = null,
    val lastActorLayerId: String? = null,
    val showSource: Boolean = true,
    val previewTimeMicros: Long = 0,
    val previewPlaying: Boolean = false,
    val presets: List<ActorMotionPreset> = emptyList(),
    val presetPreview: AppliedActorPreset? = null,
    val presetPreviewInstanceId: String? = null,
    val previewGraph: SceneGraph? = null,
    val previewDefinitionId: String? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
)

/**
 * The actor this document is authoring.
 *
 * Only the actor layer ("ac1") carries [Layer.puppetInstanceId];
 * the per-part raster layers created by auto-binding carry `boundBoneId` instead. Reading only the
 * active layer would therefore lose the actor whenever a part is selected or the animator taps
 * the canvas to sketch, and everything keyed off [actorDefinition] (the blueprint skeleton, the
 * part rail, the Performance Stage and the workflow hint) would disappear. Actor identity belongs
 * to the document, not to the current selection, so a non-puppet active layer falls back to the
 * frame's actor layer.
 *
 * Scoped deliberately to layers of THIS frame: an ordinary FxF project renders placed actors from
 * [ProjectDocument.actorPlacements], never from a layer carrying a
 * puppetInstanceId, so this fallback stays null there and project behaviour is unchanged.
 */
fun StudioState.activeActor(): PuppetInstance? {
    val frameLayers = currentFrameLayers()
    val instanceId = frameLayers.getOrNull(layers.activeLayerIndex)?.puppetInstanceId
        ?: frameLayers.firstNotNullOfOrNull { it.puppetInstanceId }
    return instanceId?.let(project.puppetInstances::get)
}

fun StudioState.actorPreviewPreset(): AppliedActorPreset? {
    val instance = activeActor()
    return actor.presetPreview.takeIf { actor.presetPreviewInstanceId == instance?.id }
        ?: instance?.motionPreset?.let(AppliedActorPreset::of)
}

fun StudioState.actorPreviewDuration(): Long {
    val instance = activeActor()
    val preset = actorPreviewPreset()
    // A repeating recipe owns its period; wrapping a 1.8 s walk at the default 2 s
    // authoring duration introduces a visible phase jump on every rehearsal loop.
    if (preset != null && instance?.artworkKeys.isNullOrEmpty() && instance?.poseKeys.isNullOrEmpty() &&
        instance?.rotationCurves.isNullOrEmpty() &&
        instance?.rootMotionCurves.isNullOrEmpty()
    ) {
        return preset.durationMicros
    }
    val requested = maxOf(
        1L,
        actor.performanceDurationMicros,
        preset?.durationMicros ?: 0L,
        instance?.artworkKeys?.maxOfOrNull { it.timeMicros } ?: 0L,
        instance?.poseKeys?.maxOfOrNull { it.timeMicros } ?: 0L,
        instance?.rotationCurves?.maxOfOrNull { it.points.lastOrNull()?.timeMicros ?: 0L } ?: 0L,
        instance?.rootMotionCurves?.maxOfOrNull { it.points.lastOrNull()?.timeMicros ?: 0L } ?: 0L,
    )
    return PerformanceDuration.expressionLoop(requested, preset)
}

/** Source drawings help construction; the pose stage must show only the moving puppet skin. */
fun StudioState.showActorSourceArtwork(): Boolean = actor.showSource && scene.studioMode != StudioMode.ANIMATE_IK &&
    actorDefinition()?.sceneGraph?.drawing?.enabled != true

fun StudioState.actorDefinition(evaluated: Boolean = false): PuppetDefinition? {
    val instance = activeActor() ?: return null
    val definition = project.puppetDefinitions[instance.definitionId] ?: return null
    if (!evaluated) return definition
    val preview = actor.previewGraph.takeIf { actor.previewDefinitionId == definition.id }
    val loop = instance.motionPlayback == PerformancePlayback.LOOP
    // Audition must not be overridden by the existing authored channels. Apply is undoable;
    // dismissing the sheet restores these channels without touching the document.
    val auditioning = actor.presetPreviewInstanceId == instance.id && actor.presetPreview != null
    val poseKeys = if (auditioning) emptyList() else instance.poseKeys
    val periodicKeys =
        loop && instance.poseKeys.firstOrNull()?.timeMicros == 0L &&
            instance.poseKeys.lastOrNull()?.timeMicros == actorPreviewDuration()
    val rotations = ActorPoseTrack.evaluate(
        poseKeys,
        actor.previewTimeMicros,
        definition.sceneGraph,
        PoseSampling(periodicKeys, instance.poseInterpolation.engineInterpolation()),
    )
    val preset =
        actor.presetPreview.takeIf { actor.presetPreviewInstanceId == instance.id }
            ?: instance.motionPreset?.let(AppliedActorPreset::of)
    val offsets = preset?.offsets(actor.previewTimeMicros).orEmpty()
    val curveSampling = CurveSampling(
        loop,
        actorPreviewDuration(),
        if (preset != null) BeforeTake.PRESERVE_PRESET else BeforeTake.HOLD_FIRST_VALUE,
    )
    val base = MotionComposition.rotations(
        definition.sceneGraph,
        instance.pose.mapValues { it.value.rotationDegrees } + rotations,
        offsets,
        PerformanceEvaluator.curveRotations(
            if (auditioning) emptyList() else instance.rotationCurves,
            actor.previewTimeMicros,
            curveSampling,
        ),
    )
    val graph = preview ?: if (scene.studioMode == StudioMode.ANIMATE_IK) {
        ActorMotionPose.apply(
            definition.sceneGraph,
            base,
            MotionComposition.rootTranslations(
                ActorPoseTrack.rootTranslations(
                    poseKeys,
                    actor.previewTimeMicros,
                    definition.sceneGraph,
                    PoseSampling(periodicKeys, instance.poseInterpolation.engineInterpolation()),
                ),
                preset?.rootTranslations(actor.previewTimeMicros, definition.sceneGraph).orEmpty(),
                RootMotionEvaluator.sample(
                    if (auditioning) emptyList() else instance.rootMotionCurves,
                    actor.previewTimeMicros,
                    curveSampling,
                ),
            ),
            ActorPoseTrack.contacts(poseKeys, actor.previewTimeMicros),
        )
    } else {
        definition.sceneGraph
    }
    return definition.copy(
        sceneGraph = ActorArtwork.apply(
            graph,
            instance.artworkKeys,
            actor.previewTimeMicros,
        ),
    )
}
