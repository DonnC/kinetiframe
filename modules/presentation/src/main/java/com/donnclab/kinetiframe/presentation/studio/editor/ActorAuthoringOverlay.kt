// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.donnclab.kinetiframe.core.document.PuppetDefinition
import com.donnclab.kinetiframe.designsystem.theme.ActorStudioTokens
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.actors.rigging.PartRail
import com.donnclab.kinetiframe.presentation.actors.sketch.ActorDrawingModeButton
import com.donnclab.kinetiframe.presentation.studio.actorstudio.rigging.ActorArtworkControls
import com.donnclab.kinetiframe.presentation.studio.actorstudio.rigging.ActorArtworkMenu
import com.donnclab.kinetiframe.presentation.studio.actorstudio.rigging.PartActionsRow
import com.donnclab.kinetiframe.presentation.studio.actorstudio.rigging.PartPickerSheet
import com.donnclab.kinetiframe.presentation.studio.actorstudio.sketch.ActorSketchArtworkControls
import com.donnclab.kinetiframe.presentation.studio.actorstudio.sketch.ActorSketchJointControls
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
        // Continuous drawing is for imported art; a drawn actor has no use for the toggle.
        if (panels.toolbarVisible && definition != null && definition.sceneGraph.figure == null &&
            !state.actor.isPerformanceAuthoring
        ) {
            Box(
                Modifier.align(
                    Alignment.TopStart,
                ).padding(start = EditorTokens.actorHintStartInset, top = Dimens.spaceS),
            ) {
                ActorDrawingModeButton(
                    definition.sceneGraph.drawing != null,
                    definition.sceneGraph.drawing?.enabled == true,
                    onSketch = commands.openSketch,
                    onMode = { commands.onIntent(StudioIntent.Actor.UseDrawing(it)) },
                )
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            // The selected part's tools share the rail's band instead of floating over the actor.
            // On a narrow screen, such as a phone held upright, they stack above a full-width rail.
            BoxWithConstraints(Modifier.fillMaxWidth().padding(bottom = Dimens.spaceL)) {
                val rail: @Composable (Modifier) -> Unit = { railModifier ->
                    if (definition != null) {
                        PartRail(
                            graph = definition.sceneGraph,
                            selectedPartId = state.scene.selectedBoneId,
                            onPartSelected = { partId -> commands.onIntent(StudioIntent.Rigging.SelectBone(partId)) },
                            modifier = railModifier,
                        )
                    }
                }
                if (maxWidth < ActorStudioTokens.partBarStackBreakpoint) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        SelectedPartControls(state, definition, ui.artwork, commands)
                        rail(Modifier.fillMaxWidth())
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SelectedPartControls(state, definition, ui.artwork, commands)
                        rail(Modifier.weight(1f))
                    }
                }
            }
        }
    }

    if (panels.toolbarVisible && host.isActorStudioSession && mode == StudioMode.ANIMATE_IK) {
        val graph = state.actorDefinition()?.sceneGraph
        val selected = state.scene.selectedBoneId
        val defaultName = stringResource(R.string.part_artwork_default)
        val choices = if (graph != null && selected != null) {
            com.donnclab.kinetiframe.actors.rig.model.ActorArtwork.choices(graph, selected, defaultName)
        } else {
            emptyList()
        }
        if (selected != null && choices.isNotEmpty()) {
            // On the stage's right edge: clear of the timeline and frame strip below.
            Box(Modifier.align(Alignment.CenterEnd).padding(end = Dimens.spaceL)) {
                ActorArtworkMenu(selected, choices, commands.onIntent)
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
            modifier = Modifier.padding(horizontal = Dimens.spaceS),
            onChooseArtwork = { artwork.choose(false, state, onIntent) },
            onEditArtwork = edit,
            onReplaceArtwork = replace,
        )
    }
    if (state.scene.studioMode == StudioMode.RASTER_DRAWING && !state.actor.isPerformanceAuthoring) {
        ActorArtworkControls(selectedBone, onIntent, onAttach = replace, onEdit = edit)
    }
}
