package com.donnclab.kinetiframe.domain.manager

import com.donnclab.kinetiframe.engine.rigging.presets.AppliedActorPreset
import com.donnclab.kinetiframe.ui.StudioMode
import com.donnclab.kinetiframe.ui.TimelineEffect
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.TimelineState
import com.donnclab.kinetiframe.ui.activeActor
import com.donnclab.kinetiframe.ui.actorDefinition

/** Audition is transient; Apply is the only operation that changes the saved recipe. */
class ActorPresetManager(
    private val getState: () -> TimelineState,
    private val updateState: ((TimelineState) -> TimelineState) -> Unit,
    private val checkpoint: () -> Unit,
    private val sendEffect: (TimelineEffect) -> Unit,
) {
    fun handle(intent: TimelineIntent.ActorPreset) {
        val state = getState()
        if (intent == TimelineIntent.ActorPreset.Cancel) {
            updateState {
                it.copy(
                    actor = it.actor.copy(
                        presetPreview = null,
                        presetPreviewInstanceId = null,
                        previewGraph = null,
                        previewTimeMicros = 0,
                    ),
                )
            }
            return
        }
        if (state.scene.studioMode != StudioMode.ANIMATE_IK) return
        val instance = state.activeActor() ?: return
        val definition = state.actorDefinition() ?: return
        when (intent) {
            is TimelineIntent.ActorPreset.Preview -> {
                val recipe = state.actor.presets.find { it.id == intent.id } ?: return
                if (!recipe.supportsView(state.actor.authoredView)) {
                    sendEffect(
                        TimelineEffect.ShowError(
                            "This preset was authored for ${recipe.authoredView.title}. Use a performance authored for this view.",
                        ),
                    )
                    return
                }
                val missing = recipe.missingBones(definition.sceneGraph.bones.keys)
                if (missing.isNotEmpty()) {
                    sendEffect(TimelineEffect.ShowError("Requires: ${missing.joinToString()}"))
                    return
                }
                updateState {
                    it.copy(
                        actor = it.actor.copy(
                            presetPreview = AppliedActorPreset(recipe),
                            presetPreviewInstanceId = instance.id,
                            previewTimeMicros = 0,
                            previewGraph = null,
                        ),
                    )
                }
            }

            is TimelineIntent.ActorPreset.SetIntensity -> if (intent.value.isFinite()) {
                updateState {
                    it.copy(
                        actor = it.actor.copy(
                            presetPreview = it.actor.presetPreview?.copy(intensity = intent.value.coerceIn(0f, 2f)),
                        ),
                    )
                }
            }

            is TimelineIntent.ActorPreset.SetSpeed -> if (intent.value.isFinite()) {
                updateState {
                    it.copy(
                        actor = it.actor.copy(
                            presetPreview = it.actor.presetPreview?.copy(speed = intent.value.coerceIn(0.25f, 3f)),
                        ),
                    )
                }
            }

            TimelineIntent.ActorPreset.Apply, TimelineIntent.ActorPreset.Remove -> {
                if (intent == TimelineIntent.ActorPreset.Apply &&
                    (state.actor.presetPreview == null || state.actor.presetPreviewInstanceId != instance.id)
                ) {
                    return
                }
                checkpoint()
                val preset = if (intent == TimelineIntent.ActorPreset.Remove) null else state.actor.presetPreview
                updateState {
                    it.copy(
                        project = it.project.copy(
                            puppetInstances = it.project.puppetInstances +
                                (
                                    instance.id to instance.copy(
                                        motionPreset = preset,
                                        poseKeys = if (preset != null) emptyList() else instance.poseKeys,
                                        rotationCurves = if (preset != null) emptyList() else instance.rotationCurves,
                                        rootMotionCurves = if (preset !=
                                            null
                                        ) {
                                            emptyList()
                                        } else {
                                            instance.rootMotionCurves
                                        },
                                    )
                                    ),
                        ),
                        actor = it.actor.copy(
                            presetPreview = null,
                            presetPreviewInstanceId = null,
                            previewGraph = null,
                            previewTimeMicros = 0,
                        ),
                    )
                }
            }

            TimelineIntent.ActorPreset.Cancel -> Unit
        }
    }
}
