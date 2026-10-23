package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.rig.presets.AppliedActorPreset
import com.donnclab.kinetiframe.studio.StudioEffect
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.activeActor
import com.donnclab.kinetiframe.studio.actorDefinition

/** Audition is transient; Apply is the only operation that changes the saved recipe. */
class ActorPresetManager(
    private val getState: () -> StudioState,
    private val updateState: ((StudioState) -> StudioState) -> Unit,
    private val checkpoint: () -> Unit,
    private val sendEffect: (StudioEffect) -> Unit,
) {
    fun handle(intent: StudioIntent.ActorPreset) {
        val state = getState()
        if (intent == StudioIntent.ActorPreset.Cancel) {
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
            is StudioIntent.ActorPreset.Preview -> {
                val recipe = state.actor.presets.find { it.id == intent.id } ?: return
                if (!recipe.supportsView(state.actor.authoredView)) {
                    sendEffect(
                        StudioEffect.ShowError(
                            "This preset was authored for ${recipe.authoredView.title}. Use a performance authored for this view.",
                        ),
                    )
                    return
                }
                val missing = recipe.missingBones(definition.sceneGraph.bones.keys)
                if (missing.isNotEmpty()) {
                    sendEffect(StudioEffect.ShowError("Requires: ${missing.joinToString()}"))
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

            is StudioIntent.ActorPreset.SetIntensity -> if (intent.value.isFinite()) {
                updateState {
                    it.copy(
                        actor = it.actor.copy(
                            presetPreview = it.actor.presetPreview?.copy(intensity = intent.value.coerceIn(0f, 2f)),
                        ),
                    )
                }
            }

            is StudioIntent.ActorPreset.SetSpeed -> if (intent.value.isFinite()) {
                updateState {
                    it.copy(
                        actor = it.actor.copy(
                            presetPreview = it.actor.presetPreview?.copy(speed = intent.value.coerceIn(0.25f, 3f)),
                        ),
                    )
                }
            }

            StudioIntent.ActorPreset.Apply, StudioIntent.ActorPreset.Remove -> {
                if (intent == StudioIntent.ActorPreset.Apply &&
                    (state.actor.presetPreview == null || state.actor.presetPreviewInstanceId != instance.id)
                ) {
                    return
                }
                checkpoint()
                val preset = if (intent == StudioIntent.ActorPreset.Remove) null else state.actor.presetPreview
                updateState {
                    it.copy(
                        project = it.project.copy(
                            puppetInstances = it.project.puppetInstances +
                                (
                                    instance.id to instance.copy(
                                        motionPreset = preset?.document(),
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

            StudioIntent.ActorPreset.Cancel -> Unit
        }
    }
}
