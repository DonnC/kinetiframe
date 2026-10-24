package com.donnclab.kinetiframe.studio.domain.actorstudio

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Path
import com.donnclab.kinetiframe.actors.rig.texture.RigAssetManager
import com.donnclab.kinetiframe.studio.PerformanceTakePhase
import com.donnclab.kinetiframe.studio.StudioEffect
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.StudioViewModel
import com.donnclab.kinetiframe.studio.activeActor
import com.donnclab.kinetiframe.studio.actorPreviewDuration
import kotlinx.coroutines.CoroutineScope

/**
 * Facade over every manager involved in actor authoring: rig setup, pose/motion rehearsal,
 * preset auditioning, sample artwork, and pivot placement. [StudioViewModel] talks to this one
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
    private val getState: () -> StudioState,
    updateState: ((StudioState) -> StudioState) -> Unit,
    private val sendEffect: (StudioEffect) -> Unit,
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

    fun handleActor(intent: StudioIntent.Actor) {
        if (intent is StudioIntent.Actor.CommitDrawing) {
            recorder.cancel()
            motion.stop()
            artwork.commitDrawing(intent)
            return
        }
        if (intent is StudioIntent.Actor.UseDrawing) {
            recorder.cancel()
            motion.stop()
            artwork.useDrawing(intent.enabled)
            return
        }
        if (intent is StudioIntent.Actor.SetDrawingOverlay) {
            recorder.cancel()
            motion.stop()
            artwork.setDrawingOverlay(intent.partId, intent.enabled, intent.behind)
            return
        }
        if (recorder.active) {
            when (intent) {
                is StudioIntent.Actor.Drag -> {
                    if (!recorder.beforeDrag(intent)) {
                        studio.handle(intent)
                        recorder.afterDrag()
                        studio.clearMotionDragBaseline()
                    }
                    return
                }

                StudioIntent.Actor.CommitDrag -> {
                    recorder.release()
                    return
                }

                StudioIntent.Actor.CancelDrag -> {
                    recorder.cancel()
                    return
                }

                is StudioIntent.Actor.SelectArtwork -> {
                    recorder.selectArtwork(intent)
                    return
                }

                else -> recorder.cancel()
            }
        }
        if (intent is StudioIntent.Actor.AttachArtwork) {
            motion.stop()
            artwork.attach(intent)
            return
        }
        if (intent is StudioIntent.Actor.SelectArtwork) {
            motion.stop()
            artwork.select(intent.partId, intent.variantId)
            return
        }
        if (intent is StudioIntent.Actor.UpdateArtwork) {
            motion.stop()
            artwork.update(intent)
            return
        }
        if (intent is StudioIntent.Actor.SetAttachmentBlend) {
            motion.stop()
            artwork.blendAttachment(intent)
            return
        }
        if (intent is StudioIntent.Actor.RemoveArtwork) {
            motion.stop()
            artwork.remove(intent)
            return
        }
        if (intent is StudioIntent.Actor.AddSampleArtwork) {
            if (getState().scene.studioMode == StudioMode.RIG_SETUP) sampleArtwork.add(intent.color)
            return
        }
        motion.stop()
        if (intent is StudioIntent.Actor.Create || intent == StudioIntent.Actor.CancelDrag) {
            preset.handle(StudioIntent.ActorPreset.Cancel)
        }
        if ((intent is StudioIntent.Actor.Drag || intent == StudioIntent.Actor.ResetPose) &&
            getState().scene.studioMode == StudioMode.ANIMATE_IK &&
            (getState().activeActor()?.motionPreset != null || getState().actor.presetPreview != null)
        ) {
            sendEffect(StudioEffect.ShowError("Remove the motion preset before recording manual poses."))
            return
        }
        if (getState().scene.pivotPlacement != null) rigging.cancelPivot()
        studio.handle(intent)
    }

    fun handleMotion(intent: StudioIntent.ActorMotion) {
        val takeIntent =
            intent == StudioIntent.ActorMotion.StartTake || intent == StudioIntent.ActorMotion.StopTake ||
                intent == StudioIntent.ActorMotion.KeepTake || intent == StudioIntent.ActorMotion.Retake ||
                intent == StudioIntent.ActorMotion.CancelTake ||
                intent is StudioIntent.ActorMotion.SetTakeSmoothness ||
                intent is StudioIntent.ActorMotion.SetTakeMoveCharacter
        if (takeIntent) {
            motion.stop()
            recorder.handle(intent)
            return
        }
        if (recorder.active &&
            !(
                getState().actor.takePhase == PerformanceTakePhase.REVIEW &&
                    (intent == StudioIntent.ActorMotion.TogglePlayback || intent is StudioIntent.ActorMotion.Scrub)
                )
        ) {
            recorder.cancel()
        }
        motion.handle(intent)
    }

    fun handlePreset(intent: StudioIntent.ActorPreset) {
        recorder.cancel()
        if (intent is StudioIntent.ActorPreset.SetSpeed && intent.value.isFinite()) {
            val before = getState()
            val fraction = before.actor.previewTimeMicros.toDouble() / before.actorPreviewDuration()
            motion.stop()
            preset.handle(intent)
            motion.handle(StudioIntent.ActorMotion.Scrub((fraction * getState().actorPreviewDuration()).toLong()))
            if (before.actor.previewPlaying) motion.handle(StudioIntent.ActorMotion.TogglePlayback)
            return
        }
        if (intent !is StudioIntent.ActorPreset.SetSpeed &&
            intent !is StudioIntent.ActorPreset.SetIntensity
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
        preset.handle(StudioIntent.ActorPreset.Cancel)
        rigging.cancelPivot()
        studio.handle(StudioIntent.Actor.CancelDrag)
    }

    fun clearHistory() = studio.clearHistory()

    fun preparePivotPlacement(sourceFrame: Bitmap, lassoPath: Path, dilationRadius: Int) =
        rigging.preparePivotPlacement(sourceFrame, lassoPath, dilationRadius)

    fun updatePivot(x: Float, y: Float) = rigging.updatePivot(x, y)

    fun addBoneAt(x: Float, y: Float) = rigging.addBoneAt(x, y)

    fun confirmPivot() = rigging.confirmPivot()

    fun cancelPivot() = rigging.cancelPivot()
}
