package com.donnclab.kinetiframe.domain.manager

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Path
import com.donnclab.kinetiframe.engine.rigging.texture.RigAssetManager
import com.donnclab.kinetiframe.ui.PerformanceTakePhase
import com.donnclab.kinetiframe.ui.StudioMode
import com.donnclab.kinetiframe.ui.TimelineEffect
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.TimelineState
import com.donnclab.kinetiframe.ui.TimelineViewModel
import com.donnclab.kinetiframe.ui.activeActor
import com.donnclab.kinetiframe.ui.actorPreviewDuration
import kotlinx.coroutines.CoroutineScope

/**
 * Facade over every manager involved in actor authoring: rig setup, pose/motion rehearsal,
 * preset auditioning, sample artwork, and pivot placement. [TimelineViewModel] talks to this one
 * class instead of wiring five separate managers itself -- that kept ballooning its own
 * constructor and, worse, meant the same "stop whatever the user was mid-gesture on" reset
 * sequence had to be copy-pasted at every call site that could interrupt authoring.
 *
 * FxF-timeline concerns (placing an actor onto the main timeline, trimming/splitting a clip) are
 * deliberately out of scope here -- see [ActorPlacementManager] -- since those aren't "authoring
 * an actor," they're arranging an already-authored performance in a scene.
 */
class ActorAuthoringCoordinator(
    scope: CoroutineScope,
    rigAssetManager: RigAssetManager,
    private val getState: () -> TimelineState,
    updateState: ((TimelineState) -> TimelineState) -> Unit,
    private val sendEffect: (TimelineEffect) -> Unit,
) {
    private val studio = ActorStudioManager(
        getState = getState,
        updateState = updateState,
        retainRuntimeAssets = rigAssetManager::retainOnly,
    )
    private val artwork = ActorArtworkManager(
        rigAssetManager,
        getState,
        updateState,
        studio::checkpoint,
        studio::reconcileRuntimeAssets,
        sendEffect,
    )
    private val motion = ActorMotionManager(
        scope,
        getState,
        updateState,
        checkpoint = { studio.checkpoint() },
        awaitFrame = {
            kotlinx.coroutines.withContext(androidx.compose.ui.platform.AndroidUiDispatcher.Main) {
                androidx.compose.runtime.withFrameNanos { }
            }
        },
    )
    private val recorder = ActorPerformanceRecorder(
        scope,
        getState,
        updateState,
        studio::checkpoint,
        sendEffect,
        awaitFrame = {
            kotlinx.coroutines.withContext(androidx.compose.ui.platform.AndroidUiDispatcher.Main) {
                androidx.compose.runtime.withFrameNanos { }
            }
        },
    )
    private val preset =
        ActorPresetManager(getState, updateState, checkpoint = { studio.checkpoint() }, sendEffect = sendEffect)
    private val sampleArtwork = ActorSampleArtwork(
        scope,
        rigAssetManager,
        getState,
        updateState,
        checkpoint = { studio.checkpoint() },
        sendEffect = sendEffect,
    )
    private val rigging = RiggingManager(
        scope = scope,
        rigAssetManager = rigAssetManager,
        getState = getState,
        updateState = updateState,
        sendEffect = sendEffect,
        checkpoint = { studio.checkpoint() },
        reconcileRuntimeAssets = { studio.reconcileRuntimeAssets() },
    )

    /** See [RiggingManager.autoBindLayerContent]. */
    fun autoBindLayerContent(boneId: String, frameBitmap: android.graphics.Bitmap) =
        rigging.autoBindLayerContent(boneId, frameBitmap)

    fun handleActor(intent: TimelineIntent.Actor) {
        if (intent is TimelineIntent.Actor.CommitDrawing) {
            recorder.cancel()
            motion.stop()
            artwork.commitDrawing(intent)
            return
        }
        if (intent is TimelineIntent.Actor.UseDrawing) {
            recorder.cancel()
            motion.stop()
            artwork.useDrawing(intent.enabled)
            return
        }
        if (intent is TimelineIntent.Actor.SetDrawingOverlay) {
            recorder.cancel()
            motion.stop()
            artwork.setDrawingOverlay(intent.partId, intent.enabled, intent.behind)
            return
        }
        if (recorder.active) {
            when (intent) {
                is TimelineIntent.Actor.Drag -> {
                    if (!recorder.beforeDrag(intent)) {
                        studio.handle(intent)
                        recorder.afterDrag()
                        studio.clearMotionDragBaseline()
                    }
                    return
                }

                TimelineIntent.Actor.CommitDrag -> {
                    recorder.release()
                    return
                }

                TimelineIntent.Actor.CancelDrag -> {
                    recorder.cancel()
                    return
                }

                is TimelineIntent.Actor.SelectArtwork -> {
                    recorder.selectArtwork(intent)
                    return
                }

                else -> recorder.cancel()
            }
        }
        if (intent is TimelineIntent.Actor.AttachArtwork) {
            motion.stop()
            artwork.attach(intent)
            return
        }
        if (intent is TimelineIntent.Actor.SelectArtwork) {
            motion.stop()
            artwork.select(intent.partId, intent.variantId)
            return
        }
        if (intent is TimelineIntent.Actor.UpdateArtwork) {
            motion.stop()
            artwork.update(intent)
            return
        }
        if (intent is TimelineIntent.Actor.SetAttachmentBlend) {
            motion.stop()
            artwork.blendAttachment(intent)
            return
        }
        if (intent is TimelineIntent.Actor.RemoveArtwork) {
            motion.stop()
            artwork.remove(intent)
            return
        }
        if (intent is TimelineIntent.Actor.AddSampleArtwork) {
            if (getState().scene.studioMode == StudioMode.RIG_SETUP) sampleArtwork.add(intent.color)
            return
        }
        motion.stop()
        if (intent is TimelineIntent.Actor.Create || intent == TimelineIntent.Actor.CancelDrag) {
            preset.handle(TimelineIntent.ActorPreset.Cancel)
        }
        if ((intent is TimelineIntent.Actor.Drag || intent == TimelineIntent.Actor.ResetPose) &&
            getState().scene.studioMode == StudioMode.ANIMATE_IK &&
            (getState().activeActor()?.motionPreset != null || getState().actor.presetPreview != null)
        ) {
            sendEffect(TimelineEffect.ShowError("Remove the motion preset before recording manual poses."))
            return
        }
        if (getState().scene.pivotPlacement != null) rigging.cancelPivot()
        studio.handle(intent)
    }

    fun handleMotion(intent: TimelineIntent.ActorMotion) {
        val takeIntent =
            intent == TimelineIntent.ActorMotion.StartTake || intent == TimelineIntent.ActorMotion.StopTake ||
                intent == TimelineIntent.ActorMotion.KeepTake || intent == TimelineIntent.ActorMotion.Retake ||
                intent == TimelineIntent.ActorMotion.CancelTake ||
                intent is TimelineIntent.ActorMotion.SetTakeSmoothness ||
                intent is TimelineIntent.ActorMotion.SetTakeMoveCharacter
        if (takeIntent) {
            motion.stop()
            recorder.handle(intent)
            return
        }
        if (recorder.active &&
            !(
                getState().actor.takePhase == PerformanceTakePhase.REVIEW &&
                    (intent == TimelineIntent.ActorMotion.TogglePlayback || intent is TimelineIntent.ActorMotion.Scrub)
                )
        ) {
            recorder.cancel()
        }
        motion.handle(intent)
    }

    fun handlePreset(intent: TimelineIntent.ActorPreset) {
        recorder.cancel()
        if (intent is TimelineIntent.ActorPreset.SetSpeed && intent.value.isFinite()) {
            val before = getState()
            val fraction = before.actor.previewTimeMicros.toDouble() / before.actorPreviewDuration()
            motion.stop()
            preset.handle(intent)
            motion.handle(TimelineIntent.ActorMotion.Scrub((fraction * getState().actorPreviewDuration()).toLong()))
            if (before.actor.previewPlaying) motion.handle(TimelineIntent.ActorMotion.TogglePlayback)
            return
        }
        if (intent !is TimelineIntent.ActorPreset.SetSpeed &&
            intent !is TimelineIntent.ActorPreset.SetIntensity
        ) {
            motion.stop()
        }
        preset.handle(intent)
    }

    /** The cross-cutting "user is leaving/interrupting whatever actor-authoring gesture was in
     *  flight" reset: cancel rehearsal playback, dismiss any preset audition, drop a pending
     *  pivot placement, and cancel an in-progress actor drag. Every call site that can pull the
     *  user out of authoring mid-gesture (switching studio mode, selecting a different layer)
     *  should go through here rather than re-deriving the sequence. */
    fun cancelInFlightAuthoring() {
        recorder.cancel()
        motion.stop()
        preset.handle(TimelineIntent.ActorPreset.Cancel)
        rigging.cancelPivot()
        studio.handle(TimelineIntent.Actor.CancelDrag)
    }

    fun clearHistory() = studio.clearHistory()

    fun preparePivotPlacement(sourceFrame: Bitmap, lassoPath: Path, dilationRadius: Int) =
        rigging.preparePivotPlacement(sourceFrame, lassoPath, dilationRadius)

    fun updatePivot(x: Float, y: Float) = rigging.updatePivot(x, y)

    fun addBoneAt(x: Float, y: Float) = rigging.addBoneAt(x, y)

    fun confirmPivot() = rigging.confirmPivot()

    fun cancelPivot() = rigging.cancelPivot()
}
