// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.actors.ui.rigging.ActorArtworkControls
import com.donnclab.kinetiframe.actors.ui.rigging.ActorArtworkMenu
import com.donnclab.kinetiframe.actors.ui.rigging.PartActionsRow
import com.donnclab.kinetiframe.actors.ui.rigging.PartPickerSheet
import com.donnclab.kinetiframe.actors.ui.rigging.PartRail
import com.donnclab.kinetiframe.actors.ui.sketch.ActorDrawingModeButton
import com.donnclab.kinetiframe.actors.ui.sketch.ActorSketchArtworkControls
import com.donnclab.kinetiframe.actors.ui.sketch.ActorSketchJointControls
import com.donnclab.kinetiframe.core.document.PuppetDefinition
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.actorDefinition

/** Actor Studio's canvas controls: the part rail and the selected part's artwork actions. */
@Composable
internal fun BoxScope.ActorAuthoringOverlay(
    state: StudioState,
    host: EditorHost,
    ui: EditorUiState,
    commands: EditorCommands,
) {
    val panels = ui.panels
    val mode = state.scene.studioMode
    // The part rail answers "which part am I working on", which is separate from the tool in
    // hand, so it stays while drawing as well as in Rig Setup. Only posing hides it.
    if (host.isActorStudioSession && mode != StudioMode.ANIMATE_IK) {
        val definition = state.actorDefinition()
        if (panels.toolbarVisible && definition != null && !state.actor.isPerformanceAuthoring) {
            Box(Modifier.align(Alignment.TopStart).padding(start = 96.dp, top = 8.dp)) {
                ActorDrawingModeButton(
                    definition.sceneGraph.drawing != null,
                    definition.sceneGraph.drawing?.enabled == true,
                    onSketch = commands.openSketch,
                    onMode = { commands.onIntent(StudioIntent.Actor.UseDrawing(it)) },
                )
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SelectedPartControls(state, definition, ui.artwork, commands)
                if (definition != null) {
                    PartRail(
                        graph = definition.sceneGraph,
                        selectedPartId = state.scene.selectedBoneId,
                        onPartSelected = { partId -> commands.onIntent(StudioIntent.Rigging.SelectBone(partId)) },
                    )
                }
            }
        }
    }

    if (panels.toolbarVisible && host.isActorStudioSession && mode == StudioMode.ANIMATE_IK) {
        state.actorDefinition()?.sceneGraph?.bones?.get(state.scene.selectedBoneId)
            ?.takeIf { it.artworkVariants.isNotEmpty() }?.let { part ->
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)) {
                    ActorArtworkMenu(part, commands.onIntent)
                }
            }
    }

    if (panels.showPartPicker) {
        state.actorDefinition()?.let { definition ->
            PartPickerSheet(
                onReferenceClick = host.actorReference?.let { reference ->
                    {
                        panels.showPartPicker = false
                        reference.menuOpen = true
                    }
                },
                state = state,
                definition = definition,
                onIntent = commands.onIntent,
                onDismiss = { panels.showPartPicker = false },
            )
        }
    }
}

/**
 * Artwork actions for the selected part. Rename and pivot actions show only in Rig Setup: they
 * edit the rig and would clutter the canvas while drawing.
 */
@Composable
private fun SelectedPartControls(
    state: StudioState,
    definition: PuppetDefinition?,
    artwork: ActorArtworkState,
    commands: EditorCommands,
) {
    val onIntent = commands.onIntent
    val selectedBone = definition?.sceneGraph?.bones?.get(state.scene.selectedBoneId) ?: return
    val drawing = definition.sceneGraph.drawing
    val replace: (String) -> Unit = { variant -> artwork.replace(variant, state, onIntent) }
    val edit: (String) -> Unit = { variant -> artwork.editing = selectedBone.id to variant }
    if (drawing?.enabled == true) {
        ActorSketchJointControls({ onIntent(StudioIntent.Actor.SetEditTarget(it)) }, commands.openSketch) {
            ActorSketchArtworkControls(
                selectedBone,
                selectedBone.id in (drawing.overlayParts + drawing.underlayParts),
                onIntent,
                replace,
                edit,
                behind = selectedBone.id in drawing.underlayParts,
            )
        }
    }
    if (state.scene.studioMode == StudioMode.RIG_SETUP && drawing?.enabled != true) {
        PartActionsRow(
            bone = selectedBone,
            setup = true,
            editTarget = state.actor.editTarget,
            canCaptureSketch = state.currentBitmap != null,
            onIntent = onIntent,
            modifier = Modifier.padding(bottom = 10.dp),
            onChooseArtwork = { artwork.choose(false, state, onIntent) },
            onEditArtwork = edit,
            onReplaceArtwork = replace,
        )
    }
    if (state.scene.studioMode == StudioMode.RASTER_DRAWING && !state.actor.isPerformanceAuthoring) {
        ActorArtworkControls(selectedBone, onIntent, onAttach = replace, onEdit = edit)
    }
}
