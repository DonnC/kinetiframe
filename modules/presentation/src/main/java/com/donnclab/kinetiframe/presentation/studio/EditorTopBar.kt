package com.donnclab.kinetiframe.presentation.studio
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Bone
import com.composables.icons.lucide.Clapperboard
import com.composables.icons.lucide.ClipboardPaste
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.Frame
import com.composables.icons.lucide.Ghost
import com.composables.icons.lucide.Grid2x2
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Maximize
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.Redo
import com.composables.icons.lucide.Save
import com.composables.icons.lucide.Share
import com.composables.icons.lucide.Spline
import com.composables.icons.lucide.Undo
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.ContextIconAction
import com.donnclab.kinetiframe.presentation.common.ContextIconButton
import com.donnclab.kinetiframe.presentation.common.IconContextMenu
import com.donnclab.kinetiframe.presentation.studio.actorstudio.rigging.StudioModePill
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.actorDefinition

/** The editor top bar's fixed buttons. A null [onHomeClick] hides Home. */
data class EditorTopBarActions(
    val onHomeClick: (() -> Unit)?,
    val onStudioClick: () -> Unit,
    val onGridSettingsClick: () -> Unit,
    val onOnionSettingsClick: () -> Unit,
)

@Composable
fun EditorTopBar(
    state: StudioState,
    onIntent: (StudioIntent) -> Unit,
    actions: EditorTopBarActions,
    modifier: Modifier = Modifier,
    /** True only for the standalone Actor Studio's own EditorScreen instance (see
     *  EditorScreen's `initialEditorState`) -- a plain FxF project never shows the Rig Setup
     *  entry point, however its data got here; adding/editing an actor performance from FxF goes
     *  through the Actor Studio destination instead (see MainActivity's onCreatePerformance /
     *  onEditPerformance navigation). Also gates the [StudioModePill] shown at the leading edge of
     *  this bar, the same phase switch [com.donnclab.kinetiframe.presentation.studio.actorstudio.rigging.PuppetTopBar] shows
     *  while sketching is paused for rigging or posing. */
    isActorStudio: Boolean = false,
    showOverflowMenu: Boolean,
    onOverflowMenuToggle: (Boolean) -> Unit,
    /** Launches the system image picker; the picked image becomes the project's persistent
     *  background (see [com.donnclab.kinetiframe.studio.domain.BackgroundManager]). */
    onSetBackgroundClick: () -> Unit = {},
    onImportFrameImage: (() -> Unit)? = null,
    /** Null when this editor has no backing project yet (e.g. an embedded Actor Studio canvas) --
     *  the menu item is hidden rather than shown disabled, since there is nothing a user can do
     *  about it from here. See ProjectRepository/EditorScreen's project-scoped save wiring. */
    onSaveProjectClick: (() -> Unit)? = null,
    /** Exports the project's current background image into the local Library, so it can be reused
     *  or shared later -- only meaningful (and only shown) once a background is actually set. */
    onAddBackgroundToLibraryClick: (() -> Unit)? = null,
    /** Flattens the current frame to a PNG and saves it to the local library. */
    onAddCurrentFrameToLibraryClick: (() -> Unit)? = null,
    /** Non-null only inside the Pack Creator. A separate menu item from "Save Project" because it
     *  writes a PACK library item from the draft's tagged frames. Sharing as a `.kpack` happens
     *  from Home's Packs tab. */
    onSavePackClick: (() -> Unit)? = null,
    onSaveActorClick: (() -> Unit)? = null,
    onFocusCanvas: (() -> Unit)? = null,
    onTracksOpen: (() -> Unit)? = null,
    onEditPackItem: (() -> Unit)? = null,
    actorViews: (@Composable () -> Unit)? = null,
    onAttachActorArtwork: ((Boolean) -> Unit)? = null,
    /** Opens export for this project, saving first; null where there is no project to export. */
    onExportClick: (() -> Unit)? = null,
    /** Opens the Library picker for background images. */
    onBackgroundFromLibraryClick: (() -> Unit)? = null,
) {
    val (onHomeClick, onStudioClick, onGridSettingsClick, onOnionSettingsClick) = actions
    val context = androidx.compose.ui.platform.LocalContext.current
    var assetsExpanded by remember { mutableStateOf(false) }
    var speedOpen by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(EditorTokens.topBarHeight),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        contentColor = EditorTokens.dark,
        shape = RoundedCornerShape(bottomStart = Dimens.radiusL, bottomEnd = Dimens.radiusL),
        tonalElevation = Dimens.elevationFlat,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.spaceXs),
            verticalAlignment = Alignment.CenterVertically,
            // The drawing bar spreads its tools evenly; Actor Studio packs its own controls from the start.
            horizontalArrangement = if (isActorStudio) Arrangement.Start else Arrangement.SpaceEvenly,
        ) {
            if (isActorStudio) {
                onHomeClick?.let { home ->
                    ContextIconButton(ContextIconAction(Icons.Filled.Home, stringResource(R.string.editor_home), home))
                }
                StudioModePill(
                    current = state.scene.studioMode,
                    onSelect = { onIntent(StudioIntent.Rigging.SetStudioMode(it)) },
                    modifier = Modifier.padding(end = Dimens.spaceS),
                )
            }
            if (!isActorStudio) {
                DrawingBarTools(state, onIntent, onHomeClick)
            } else {
                Row(
                    modifier = Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Setup is reached from the mode switch beside this row; no second bone button.
                    com.donnclab.kinetiframe.presentation.actors.rigging.ActorToolButton(
                        Lucide.Undo,
                        stringResource(R.string.editor_undo),
                    ) {
                        onIntent(StudioIntent.Draw.Undo)
                    }
                    com.donnclab.kinetiframe.presentation.actors.rigging.ActorToolButton(
                        Lucide.Redo,
                        stringResource(R.string.editor_redo),
                    ) {
                        onIntent(StudioIntent.Draw.Redo)
                    }
                }
            }

            if (isActorStudio) {
                actorViews?.invoke()
                onAttachActorArtwork?.let { choose ->
                    com.donnclab.kinetiframe.presentation.actors.rigging.ActorPackAttachmentButton(
                        choose,
                        enabled =
                        state.scene.selectedBoneId != null,
                    )
                }
                onSaveActorClick?.let { save ->
                    com.donnclab.kinetiframe.presentation.actors.rigging.ActorToolButton(
                        Lucide.Save,
                        stringResource(R.string.puppet_save_revision),
                        onClick = save,
                    )
                }
            }
            // Projects autosave, so their Save lives in the menu; Pack Creator saves only on request.
            onSavePackClick?.let { save ->
                IconButton(onClick = save) {
                    Icon(Lucide.Save, stringResource(R.string.action_save), tint = EditorTokens.dark)
                }
            }
            onTracksOpen?.let { open ->
                ContextIconButton(
                    ContextIconAction(Icons.Filled.ViewTimeline, stringResource(R.string.editor_asset_tracks), open),
                )
            }
            onEditPackItem?.let { edit ->
                ContextIconButton(
                    ContextIconAction(Icons.Filled.EditNote, stringResource(R.string.editor_pack_item_details), edit),
                )
            }
            Box {
                if (isActorStudio) {
                    com.donnclab.kinetiframe.presentation.actors.rigging.ActorToolButton(
                        Lucide.EllipsisVertical,
                        stringResource(R.string.editor_more_settings),
                    ) {
                        onOverflowMenuToggle(true)
                    }
                } else {
                    IconButton(onClick = {
                        onOverflowMenuToggle(true)
                    }) {
                        Icon(
                            Lucide.EllipsisVertical,
                            stringResource(R.string.editor_more_settings),
                            tint = EditorTokens.dark,
                        )
                    }
                }
                IconContextMenu(
                    showOverflowMenu,
                    { onOverflowMenuToggle(false) },
                    buildList {
                        // Finishing an animation ends here, so export leads the menu.
                        onExportClick?.let { export ->
                            add(
                                ContextIconAction(Lucide.Share, stringResource(R.string.editor_export_video), {
                                    onOverflowMenuToggle(false)
                                    export()
                                }),
                            )
                        }
                        onSaveProjectClick?.let { save ->
                            add(
                                ContextIconAction(Lucide.Save, stringResource(R.string.editor_save_project), {
                                    onOverflowMenuToggle(false)
                                    save()
                                }),
                            )
                        }
                        onFocusCanvas?.takeIf { !isActorStudio }?.let { focus ->
                            add(
                                ContextIconAction(Lucide.Eye, stringResource(R.string.timeline_focus_canvas), {
                                    onOverflowMenuToggle(false)
                                    focus()
                                }),
                            )
                        }
                        add(
                            ContextIconAction(Lucide.Clapperboard, stringResource(R.string.library_kind_frames), {
                                onOverflowMenuToggle(false)
                                onStudioClick()
                            }),
                        )
                        if (state.isCanvasTransformed) {
                            add(
                                ContextIconAction(Lucide.Maximize, stringResource(R.string.editor_reset_view), {
                                    onOverflowMenuToggle(false)
                                    onIntent(StudioIntent.View.ResetCanvasTransform)
                                }),
                            )
                        }
                        add(
                            ContextIconAction(Icons.Filled.Image, stringResource(R.string.editor_background), {
                                onOverflowMenuToggle(false)
                                assetsExpanded =
                                    true
                            }),
                        )
                        add(
                            ContextIconAction(Icons.Filled.Speed, stringResource(R.string.editor_frame_rate), {
                                onOverflowMenuToggle(false)
                                speedOpen = true
                            }),
                        )
                        if (state.actorDefinition() != null) {
                            add(
                                ContextIconAction(Lucide.Bone, stringResource(R.string.puppet_skeleton_overlay), {
                                    onOverflowMenuToggle(false)
                                    onIntent(StudioIntent.Rigging.ToggleSkeletonOverlay)
                                }, selected = state.scene.skeletonOverlayVisible),
                            )
                        }
                        add(
                            ContextIconAction(Lucide.Grid2x2, stringResource(R.string.grid_title), {
                                onOverflowMenuToggle(false)
                                onGridSettingsClick()
                            }),
                        )
                        add(
                            ContextIconAction(Lucide.Ghost, stringResource(R.string.editor_onion_skin), {
                                onOverflowMenuToggle(false)
                                onOnionSettingsClick()
                            }),
                        )
                        onImportFrameImage?.let { import ->
                            add(
                                ContextIconAction(
                                    Icons.Filled.AddPhotoAlternate,
                                    stringResource(
                                        R.string.editor_frame_image,
                                    ),
                                    {
                                        onOverflowMenuToggle(false)
                                        import()
                                    },
                                ),
                            )
                        }
                    },
                )
                EditorAssetMenu(
                    assetsExpanded,
                    { assetsExpanded = false },
                    state.project.backgroundAssetId != null,
                    BackgroundMenuActions(
                        onChoose = onSetBackgroundClick,
                        onRemove = { onIntent(StudioIntent.Background.Remove) },
                        onSaveBackground = onAddBackgroundToLibraryClick,
                        onSaveFrame = onAddCurrentFrameToLibraryClick,
                        onToggleBackground = { onIntent(StudioIntent.Background.ToggleVisibility) },
                        onChooseFromLibrary = onBackgroundFromLibraryClick,
                    ),
                    state.canvas.backgroundVisible,
                )
            }
        }
    }
    if (speedOpen) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { speedOpen = false },
            title = { Text(stringResource(R.string.editor_frame_rate)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceS)) {
                    Text(stringResource(R.string.editor_frame_rate_note), style = MaterialTheme.typography.bodySmall)
                    com.donnclab.kinetiframe.presentation.common.FrameRatePicker(
                        state.animation.fps,
                        { onIntent(StudioIntent.Playback.SetFps(it)) },
                    )
                }
            },
            confirmButton = {
                ContextIconButton(
                    ContextIconAction(Icons.Filled.Check, stringResource(R.string.action_done), {
                        speedOpen =
                            false
                    }),
                )
            },
        )
    }
}

/**
 * The drawing studio's everyday tools, laid straight into the evenly spaced bar: Home, Undo, Redo,
 * Copy, Paste and the safe-frame guide. Less frequent actions, such as Save and Focus, are in the menu.
 */
@Composable
private fun DrawingBarTools(state: StudioState, onIntent: (StudioIntent) -> Unit, onHomeClick: (() -> Unit)?) {
    onHomeClick?.let { home ->
        ContextIconButton(ContextIconAction(Icons.Filled.Home, stringResource(R.string.editor_home), home))
    }
    IconButton(onClick = { onIntent(StudioIntent.Draw.Undo) }) {
        Icon(Lucide.Undo, stringResource(R.string.editor_undo), tint = EditorTokens.dark)
    }
    IconButton(onClick = { onIntent(StudioIntent.Draw.Redo) }) {
        Icon(Lucide.Redo, stringResource(R.string.editor_redo), tint = EditorTokens.muted)
    }
    ContextIconButton(
        ContextIconAction(Lucide.Copy, stringResource(R.string.editor_copy), { onIntent(StudioIntent.Selection.Copy) }),
    )
    ContextIconButton(
        ContextIconAction(Lucide.ClipboardPaste, stringResource(R.string.editor_paste), {
            onIntent(StudioIntent.Selection.Paste)
        }),
    )
    ContextIconButton(
        ContextIconAction(
            Lucide.Frame,
            stringResource(R.string.editor_safe_frame),
            { onIntent(StudioIntent.View.ToggleSafeFrame) },
            selected = state.canvas.showSafeFrame,
        ),
    )
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun EditorTopBarPreview() {
    EditorTopBar(
        StudioState(),
        {},
        EditorTopBarActions({}, {}, {}, {}),
        showOverflowMenu = false,
        onOverflowMenuToggle = {},
        onFocusCanvas = {},
    )
}
