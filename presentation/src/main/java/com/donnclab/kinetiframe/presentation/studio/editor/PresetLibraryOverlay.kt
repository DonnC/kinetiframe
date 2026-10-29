// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.editor

import androidx.compose.runtime.Composable
import com.donnclab.kinetiframe.presentation.actors.rigging.PresetLibrarySheet
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.activeActor
import com.donnclab.kinetiframe.studio.actorDefinition

/**
 * Auditions and applies motion presets for the active actor. Only presets that support the
 * authored view and use bones the actor has are offered. Preview playback stops before any
 * change, so the next preview starts from the beginning.
 */
@Composable
internal fun PresetLibraryOverlay(state: StudioState, onIntent: (StudioIntent) -> Unit, onClose: () -> Unit) {
    val actor = state.actor
    val stopPreview = { if (actor.previewPlaying) onIntent(StudioIntent.ActorMotion.TogglePlayback) }
    val boneIds = state.actorDefinition()?.sceneGraph?.bones?.keys.orEmpty()
    val activeActor = state.activeActor()
    PresetLibrarySheet(
        presets = actor.presets.filter { it.supportsView(actor.authoredView) && it.missingBones(boneIds).isEmpty() },
        previewingId = actor.presetPreview?.recipe?.id,
        onPreview = {
            stopPreview()
            onIntent(StudioIntent.ActorPreset.Preview(it))
            onIntent(StudioIntent.ActorMotion.TogglePlayback)
        },
        onApply = {
            stopPreview()
            onIntent(StudioIntent.ActorPreset.Apply)
            onClose()
        },
        canApply = actor.presetPreview != null,
        preview = actor.presetPreview,
        playing = actor.previewPlaying,
        replacesKeys = !activeActor?.poseKeys.isNullOrEmpty() || !activeActor?.rotationCurves.isNullOrEmpty(),
        onPlay = { onIntent(StudioIntent.ActorMotion.TogglePlayback) },
        onIntensity = { onIntent(StudioIntent.ActorPreset.SetIntensity(it)) },
        onSpeed = {
            stopPreview()
            onIntent(StudioIntent.ActorPreset.SetSpeed(it))
            onIntent(StudioIntent.ActorMotion.Scrub(0))
            onIntent(StudioIntent.ActorMotion.TogglePlayback)
        },
        onRemove = if (activeActor?.motionPreset != null) {
            {
                stopPreview()
                onIntent(StudioIntent.ActorPreset.Remove)
                onClose()
            }
        } else {
            null
        },
        onDismiss = {
            stopPreview()
            onIntent(StudioIntent.ActorPreset.Cancel)
            onClose()
        },
    )
}
