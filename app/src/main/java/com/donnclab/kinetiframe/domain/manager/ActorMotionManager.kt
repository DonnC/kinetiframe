package com.donnclab.kinetiframe.domain.manager

import com.donnclab.kinetiframe.engine.rigging.model.ActorPoseKey
import com.donnclab.kinetiframe.engine.rigging.model.ActorPoseTrack
import com.donnclab.kinetiframe.ui.PerformanceTakePhase
import com.donnclab.kinetiframe.ui.StudioMode
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.TimelineState
import com.donnclab.kinetiframe.ui.activeActor
import com.donnclab.kinetiframe.ui.actorDefinition
import com.donnclab.kinetiframe.ui.actorPreviewDuration
import com.donnclab.kinetiframe.ui.actorPreviewPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Local actor rehearsal clock. Scene playback stays paused while a blueprint is being edited. */
class ActorMotionManager(
    private val scope: CoroutineScope,
    private val getState: () -> TimelineState,
    private val updateState: ((TimelineState) -> TimelineState) -> Unit,
    private val checkpoint: () -> Unit,
    private val clockMicros: () -> Long = { System.nanoTime() / 1000 },
    private val awaitFrame: suspend () -> Unit = { delay(16) },
) {
    private var playback: Job? = null
    fun stop() {
        playback?.cancel()
        playback = null
        updateState { it.copy(actor = it.actor.copy(previewPlaying = false)) }
    }

    fun handle(intent: TimelineIntent.ActorMotion) {
        if (getState().scene.studioMode != StudioMode.ANIMATE_IK || getState().activeActor() == null) return
        when (intent) {
            TimelineIntent.ActorMotion.StartTake, TimelineIntent.ActorMotion.StopTake,
            TimelineIntent.ActorMotion.KeepTake, TimelineIntent.ActorMotion.Retake,
            TimelineIntent.ActorMotion.CancelTake, is TimelineIntent.ActorMotion.SetTakeSmoothness,
            is TimelineIntent.ActorMotion.SetTakeMoveCharacter,
            -> Unit

            TimelineIntent.ActorMotion.DeleteArtworkKey -> {
                stop()
                val state = getState()
                val instance = state.activeActor() ?: return
                if (instance.artworkKeys.none { it.timeMicros == state.actor.previewTimeMicros }) return
                checkpoint()
                updateState {
                    it.copy(
                        project = it.project.copy(
                            puppetInstances = it.project.puppetInstances +
                                (
                                    instance.id to
                                        instance.copy(
                                            artworkKeys = instance.artworkKeys.filterNot { key ->
                                                key.timeMicros ==
                                                    state.actor.previewTimeMicros
                                            },
                                        )
                                    ),
                        ),
                    )
                }
            }

            is TimelineIntent.ActorMotion.SetInterpolation -> {
                val instance = getState().activeActor() ?: return
                if (instance.motionPreset != null || getState().actor.presetPreview != null) return
                if (instance.poseInterpolation == intent.value &&
                    instance.rotationCurves.all { it.interpolation == intent.value } &&
                    instance.rootMotionCurves.all { it.interpolation == intent.value }
                ) {
                    return
                }
                stop()
                checkpoint()
                updateState { state ->
                    state.copy(
                        project = state.project.copy(
                            puppetInstances =
                            state.project.puppetInstances +
                                (
                                    instance.id to instance.copy(
                                        poseInterpolation = intent.value,
                                        rotationCurves = instance.rotationCurves.map {
                                            it.copy(interpolation = intent.value)
                                        },
                                        rootMotionCurves = instance.rootMotionCurves.map {
                                            it.copy(interpolation = intent.value)
                                        },
                                    )
                                    ),
                        ),
                        actor = state.actor.copy(previewGraph = null),
                    )
                }
            }

            is TimelineIntent.ActorMotion.Scrub -> {
                stop()
                updateState {
                    it.copy(
                        actor = it.actor.copy(
                            previewTimeMicros = intent.timeMicros.coerceIn(0, it.actorPreviewDuration()),
                            previewGraph = null,
                        ),
                    )
                }
            }

            is TimelineIntent.ActorMotion.SetDuration -> {
                stop()
                val current = getState()
                val lastKeyTime = maxOf(
                    current.activeActor()?.poseKeys?.maxOfOrNull { it.timeMicros } ?: 0L,
                    current.activeActor()?.rotationCurves?.maxOfOrNull { it.points.last().timeMicros } ?: 0L,
                    current.activeActor()?.rootMotionCurves?.maxOfOrNull { it.points.last().timeMicros } ?: 0L,
                    current.activeActor()?.artworkKeys?.maxOfOrNull { it.timeMicros } ?: 0L,
                )
                val requested = intent.durationMicros.coerceIn(maxOf(1L, lastKeyTime), MAX_DURATION_MICROS)
                if (requested == current.actor.performanceDurationMicros) return
                checkpoint()
                updateState { state ->
                    val lastKey = maxOf(
                        state.activeActor()?.poseKeys?.maxOfOrNull { it.timeMicros } ?: 0L,
                        state.activeActor()?.rotationCurves?.maxOfOrNull { it.points.last().timeMicros } ?: 0L,
                        state.activeActor()?.rootMotionCurves?.maxOfOrNull { it.points.last().timeMicros } ?: 0L,
                        state.activeActor()?.artworkKeys?.maxOfOrNull { it.timeMicros } ?: 0L,
                    )
                    val duration = intent.durationMicros.coerceIn(maxOf(1L, lastKey), MAX_DURATION_MICROS)
                    state.copy(
                        actor = state.actor.copy(
                            performanceDurationMicros = duration,
                            previewTimeMicros = state.actor.previewTimeMicros.coerceAtMost(duration),
                            previewGraph = null,
                        ),
                    )
                }
            }

            TimelineIntent.ActorMotion.TogglePlayback -> {
                if (getState().actor.previewPlaying) {
                    stop()
                    return
                }
                if (getState().activeActor()?.poseKeys.isNullOrEmpty() &&
                    getState().activeActor()?.artworkKeys.isNullOrEmpty() &&
                    getState().activeActor()?.rotationCurves.isNullOrEmpty() &&
                    getState().activeActor()?.rootMotionCurves.isNullOrEmpty() &&
                    getState().activeActor()?.motionPreset == null &&
                    getState().actor.presetPreview == null
                ) {
                    return
                }
                updateState { it.copy(actor = it.actor.copy(previewPlaying = true, previewGraph = null)) }
                playback = scope.launch {
                    val state = getState()
                    val review = state.actor.takePhase == PerformanceTakePhase.REVIEW
                    val lower = if (review) state.actor.takeStartMicros else 0L
                    val duration = if (review) {
                        state.actor.takeEndMicros.coerceAtLeast(
                            lower + 1,
                        )
                    } else {
                        state.actorPreviewDuration()
                    }
                    val loop =
                        review ||
                            (
                                state.actorPreviewPreset()?.recipe?.loop
                                    ?: (
                                        state.activeActor()?.motionPlayback ==
                                            com.donnclab.kinetiframe.data.actor.PerformancePlayback.LOOP
                                        )
                                )
                    val initial = if (review) {
                        state.actor.previewTimeMicros.takeIf { it in lower until duration }
                            ?: lower
                    } else {
                        state.actor.previewTimeMicros.takeIf { loop || it < duration } ?: 0L
                    }
                    val started = clockMicros()
                    while (isActive) {
                        val elapsed = (clockMicros() - started).coerceAtLeast(0)
                        val time = initial + elapsed
                        val finished = !loop && time >= duration
                        updateState {
                            it.copy(
                                actor = it.actor.copy(
                                    previewTimeMicros = if (loop) {
                                        lower +
                                            (time - lower) % (duration - lower)
                                    } else {
                                        time.coerceAtMost(duration)
                                    },
                                    previewPlaying = !finished,
                                ),
                            )
                        }
                        if (finished) break
                        awaitFrame()
                    }
                }
            }

            TimelineIntent.ActorMotion.RecordKey,
            TimelineIntent.ActorMotion.DeleteKey,
            TimelineIntent.ActorMotion.ToggleContact,
            -> {
                stop()
                val state = getState()
                if (state.activeActor()?.motionPreset != null || state.actor.presetPreview != null) return
                val instance = state.activeActor() ?: return
                val graph = state.actorDefinition(true)?.sceneGraph ?: return
                val rest = state.actorDefinition()?.sceneGraph ?: return
                var contacts = ActorPoseTrack.contacts(instance.poseKeys, state.actor.previewTimeMicros)
                if (intent == TimelineIntent.ActorMotion.ToggleContact) {
                    val id =
                        com.donnclab.kinetiframe.engine.rigging.math.ActorMotionPose.contactBone(
                            graph,
                            state.scene.selectedBoneId,
                        )
                            ?: return
                    contacts = if (id in contacts) {
                        contacts - id
                    } else {
                        contacts + (
                            id to
                                com.donnclab.kinetiframe.engine.rigging.math.ActorMotionPose.captureContact(
                                    graph,
                                    id,
                                    state.scene.selectedBoneId,
                                )
                            )
                    }
                }
                checkpoint()
                val keys = if (intent == TimelineIntent.ActorMotion.DeleteKey) {
                    instance.poseKeys.filterNot { it.timeMicros == state.actor.previewTimeMicros }
                } else {
                    ActorPoseTrack.put(
                        instance.poseKeys,
                        ActorPoseTrack.capture(state.actor.previewTimeMicros, graph, rest, contacts),
                    )
                }
                updateState {
                    it.copy(
                        project = it.project.copy(
                            puppetInstances = it.project.puppetInstances +
                                (
                                    instance.id to instance.copy(
                                        poseKeys = keys,
                                        rootMotionCurves = if (intent == TimelineIntent.ActorMotion.DeleteKey) {
                                            instance.rootMotionCurves.mapNotNull { curve ->
                                                curve.points.filterNot {
                                                    it.timeMicros ==
                                                        state.actor.previewTimeMicros
                                                }
                                                    .takeIf { it.isNotEmpty() }?.let { curve.copy(points = it) }
                                            }
                                        } else {
                                            com.donnclab.kinetiframe.domain.actor.PerformanceAuthoring.refineRoots(
                                                instance.rootMotionCurves,
                                                ActorPoseTrack.capture(
                                                    state.actor.previewTimeMicros,
                                                    graph,
                                                    rest,
                                                    contacts,
                                                ),
                                            )
                                        },
                                        rotationCurves = if (intent == TimelineIntent.ActorMotion.DeleteKey) {
                                            instance.rotationCurves.mapNotNull { curve ->
                                                curve.points.filterNot {
                                                    it.timeMicros ==
                                                        state.actor.previewTimeMicros
                                                }
                                                    .takeIf { it.isNotEmpty() }?.let { curve.copy(points = it) }
                                            }
                                        } else {
                                            com.donnclab.kinetiframe.domain.actor.PerformanceAuthoring.refineCurves(
                                                instance.rotationCurves,
                                                ActorPoseTrack.capture(
                                                    state.actor.previewTimeMicros,
                                                    graph,
                                                    rest,
                                                    contacts,
                                                ),
                                            )
                                        },
                                    )
                                    ),
                        ),
                    )
                }
            }
        }
    }

    companion object {
        const val DURATION_MICROS = 2_000_000L
        const val MAX_DURATION_MICROS = com.donnclab.kinetiframe.data.actor.PerformanceLimits.MAX_DURATION_MICROS
    }
}
