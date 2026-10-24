package com.donnclab.kinetiframe.actors.ui

import com.donnclab.kinetiframe.actors.rig.presets.AppliedActorPreset
import com.donnclab.kinetiframe.core.actor.artwork.ActorArtworkKey
import com.donnclab.kinetiframe.core.actor.document.ActorManifest
import com.donnclab.kinetiframe.core.actor.document.ActorRevision
import com.donnclab.kinetiframe.core.actor.document.PerformanceDocument
import com.donnclab.kinetiframe.core.actor.motion.ActorPoseKey
import com.donnclab.kinetiframe.core.actor.motion.CurveInterpolation
import com.donnclab.kinetiframe.core.actor.motion.RootMotionCurve
import com.donnclab.kinetiframe.core.actor.motion.RotationCurve
import com.donnclab.kinetiframe.core.actor.rigging.ActorView
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.ui.ViewEffect
import com.donnclab.kinetiframe.ui.ViewIntent
import com.donnclab.kinetiframe.ui.ViewState

data class ActorStudioDocumentState(
    val loading: Boolean = true,
    /** Saving must not unmount the live canvas: failed writes preserve unsaved edits. */
    val isSaving: Boolean = false,
    val manifest: ActorManifest? = null,
    val revision: ActorRevision? = null,
    val performance: PerformanceDocument? = null,
    /** True only when the shared canvas adapter differs from the persisted document. */
    val isDirty: Boolean = false,
    val error: String? = null,
) : ViewState

sealed interface ActorStudioDocumentIntent : ViewIntent {
    data object Reload : ActorStudioDocumentIntent
    data class SelectView(val view: ActorView) : ActorStudioDocumentIntent
    data class CreateView(val view: ActorView) : ActorStudioDocumentIntent
    data class SetDirty(val isDirty: Boolean) : ActorStudioDocumentIntent
    data object RequestExit : ActorStudioDocumentIntent
    data object DiscardAndExit : ActorStudioDocumentIntent
    data class SaveGraph(
        val sceneGraph: SceneGraph,
        val revisionId: String,
        val artworkAssetPaths: Map<String, String> = emptyMap(),
        val exitAfterSaving: Boolean = false,
    ) : ActorStudioDocumentIntent

    /** [motionPreset] is the live-applied procedural recipe (if any) from the instance being
     *  saved -- it must be threaded through here and into the saved [PerformanceDocument] as
     *  [PerformanceDocument.presetSnapshot], otherwise a performance saved while a preset is
     *  applied but never separately keyframed would persist as silently motionless. */
    data class SavePerformance(
        val name: String,
        val poseKeys: List<ActorPoseKey>,
        val motionPreset: AppliedActorPreset? = null,
        val durationMicros: Long = 1_000_000L,
        val rotationCurves: List<RotationCurve> = emptyList(),
        val poseInterpolation: CurveInterpolation = CurveInterpolation.SMOOTH,
        val artworkKeys: List<ActorArtworkKey> = emptyList(),
        val rootMotionCurves: List<RootMotionCurve> = emptyList(),
    ) : ActorStudioDocumentIntent
    data class DuplicatePerformance(val name: String) : ActorStudioDocumentIntent
    data object ArchivePerformance : ActorStudioDocumentIntent
}

sealed interface ActorStudioDocumentEffect : ViewEffect {
    data object Exit : ActorStudioDocumentEffect
    data object ConfirmDiscard : ActorStudioDocumentEffect
    data class PerformanceSaved(val document: PerformanceDocument) : ActorStudioDocumentEffect
    data class ShowError(val message: String) : ActorStudioDocumentEffect
}
