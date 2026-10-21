package com.donnclab.kinetiframe.ui.components
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Bone
import com.composables.icons.lucide.Clapperboard
import com.composables.icons.lucide.ClipboardPaste
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.Ghost
import com.composables.icons.lucide.Grid2x2
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Maximize
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.Redo
import com.composables.icons.lucide.Save
import com.composables.icons.lucide.Spline
import com.composables.icons.lucide.Undo
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.TimelineState
import com.donnclab.kinetiframe.ui.actorDefinition
import com.donnclab.kinetiframe.ui.components.common.ContextIconAction
import com.donnclab.kinetiframe.ui.components.common.ContextIconButton
import com.donnclab.kinetiframe.ui.components.common.IconContextMenu
import com.donnclab.kinetiframe.ui.components.rigging.StudioModePill

@Composable
fun EditorTopBar(
    state: TimelineState,
    onIntent: (TimelineIntent) -> Unit,
    onHomeClick: (() -> Unit)?,
    onStudioClick: () -> Unit,
    onGridSettingsClick: () -> Unit,
    onOnionSettingsClick: () -> Unit,
    /** True only for the standalone Actor Studio's own EditorScreen instance (see
     *  EditorScreen's `initialEditorState`) -- a plain FxF project never shows the Rig Setup
     *  entry point, however its data got here; adding/editing an actor performance from FxF goes
     *  through the Actor Studio destination instead (see MainActivity's onCreatePerformance /
     *  onEditPerformance navigation). Also gates the [StudioModePill] shown at the leading edge of
     *  this bar, the same phase switch [com.donnclab.kinetiframe.ui.components.rigging.PuppetTopBar] shows
     *  while sketching is paused for rigging or posing. */
    isActorStudio: Boolean = false,
    showOverflowMenu: Boolean,
    onOverflowMenuToggle: (Boolean) -> Unit,
    /** Launches the system image picker; the picked image becomes the project's persistent
     *  background (see [com.donnclab.kinetiframe.domain.manager.BackgroundManager]). */
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
    modifier: Modifier = Modifier,
    onFocusCanvas: (() -> Unit)? = null,
    onTracksOpen: (() -> Unit)? = null,
    onEditPackItem: (() -> Unit)? = null,
    actorViews: (@Composable () -> Unit)? = null,
    onAttachActorArtwork: ((Boolean) -> Unit)? = null,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var assetsExpanded by remember { mutableStateOf(false) }
    var speedOpen by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        contentColor = EditorTokens.dark,
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isActorStudio) {
                onHomeClick?.let { home -> ContextIconButton(ContextIconAction(Icons.Filled.Home, "Home", home)) }
                StudioModePill(
                    current = state.scene.studioMode,
                    onSelect = { onIntent(TimelineIntent.Rigging.SetStudioMode(it)) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Row(
                modifier = Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isActorStudio) {
                    onHomeClick?.let { home ->
                        ContextIconButton(ContextIconAction(Icons.Filled.Home, "Home", home))
                    }
                }
                if (isActorStudio) {
                    com.donnclab.kinetiframe.ui.components.rigging.ActorToolButton(
                        Lucide.Bone,
                        "Edit skeleton joints",
                    ) {
                        onIntent(TimelineIntent.Rigging.SetStudioMode(com.donnclab.kinetiframe.ui.StudioMode.RIG_SETUP))
                        onIntent(
                            TimelineIntent.Actor.SetEditTarget(com.donnclab.kinetiframe.ui.ActorEditTarget.JOINT_END),
                        )
                    }
                    com.donnclab.kinetiframe.ui.components.rigging.ActorToolButton(Lucide.Undo, "Undo") {
                        onIntent(TimelineIntent.Draw.Undo)
                    }
                    com.donnclab.kinetiframe.ui.components.rigging.ActorToolButton(Lucide.Redo, "Redo") {
                        onIntent(TimelineIntent.Draw.Redo)
                    }
                } else {
                    IconButton(onClick = {
                        onIntent(TimelineIntent.Draw.Undo)
                    }) { Icon(Lucide.Undo, "Undo", tint = EditorTokens.dark) }
                    IconButton(onClick = {
                        onIntent(TimelineIntent.Draw.Redo)
                    }) { Icon(Lucide.Redo, "Redo", tint = EditorTokens.muted) }
                }
            }

            if (isActorStudio) {
                actorViews?.invoke()
                onAttachActorArtwork?.let { choose ->
                    com.donnclab.kinetiframe.ui.components.rigging.ActorPackAttachmentButton(
                        choose,
                        enabled =
                        state.scene.selectedBoneId != null,
                    )
                }
                onSaveActorClick?.let { save ->
                    com.donnclab.kinetiframe.ui.components.rigging.ActorToolButton(
                        Lucide.Save,
                        "Save actor revision",
                        onClick = save,
                    )
                }
            }
            (onSavePackClick ?: onSaveProjectClick)?.let { save ->
                IconButton(onClick = save) { Icon(Lucide.Save, "Save", tint = EditorTokens.dark) }
            }
            onTracksOpen?.let { open ->
                ContextIconButton(ContextIconAction(Icons.Filled.ViewTimeline, "Edit asset tracks", open))
            }
            onEditPackItem?.let { edit ->
                ContextIconButton(ContextIconAction(Icons.Filled.EditNote, "Edit pack item details", edit))
            }
            onFocusCanvas?.takeIf { !isActorStudio }?.let { focus ->
                ContextIconButton(ContextIconAction(com.composables.icons.lucide.Lucide.Eye, "Focus canvas", focus))
            }
            Box {
                if (isActorStudio) {
                    com.donnclab.kinetiframe.ui.components.rigging.ActorToolButton(
                        Lucide.EllipsisVertical,
                        "More Settings",
                    ) {
                        onOverflowMenuToggle(true)
                    }
                } else {
                    IconButton(onClick = {
                        onOverflowMenuToggle(true)
                    }) { Icon(Lucide.EllipsisVertical, "More Settings", tint = EditorTokens.dark) }
                }
                IconContextMenu(
                    showOverflowMenu,
                    { onOverflowMenuToggle(false) },
                    buildList {
                        add(
                            ContextIconAction(Lucide.Copy, "Copy", {
                                onOverflowMenuToggle(false)
                                onIntent(TimelineIntent.Selection.Copy)
                            }),
                        )
                        add(
                            ContextIconAction(Lucide.ClipboardPaste, "Paste", {
                                onOverflowMenuToggle(false)
                                onIntent(TimelineIntent.Selection.Paste)
                            }),
                        )
                        add(
                            ContextIconAction(Lucide.Clapperboard, "Frames", {
                                onOverflowMenuToggle(false)
                                onStudioClick()
                            }),
                        )
                        if (state.isCanvasTransformed) {
                            add(
                                ContextIconAction(Lucide.Maximize, "Reset view", {
                                    onOverflowMenuToggle(false)
                                    onIntent(TimelineIntent.View.ResetCanvasTransform)
                                }),
                            )
                        }
                        add(
                            ContextIconAction(Icons.Filled.Image, "Background", {
                                onOverflowMenuToggle(false)
                                assetsExpanded =
                                    true
                            }),
                        )
                        add(
                            ContextIconAction(Icons.Filled.Speed, "Speed", {
                                onOverflowMenuToggle(false)
                                speedOpen = true
                            }),
                        )
                        if (state.actorDefinition() != null) {
                            add(
                                ContextIconAction(Lucide.Bone, "Skeleton overlay", {
                                    onOverflowMenuToggle(false)
                                    onIntent(TimelineIntent.Rigging.ToggleSkeletonOverlay)
                                }, selected = state.scene.skeletonOverlayVisible),
                            )
                        }
                        add(
                            ContextIconAction(Lucide.Maximize, "Safe frame", {
                                onIntent(TimelineIntent.View.ToggleSafeFrame)
                                onOverflowMenuToggle(false)
                            }, selected = state.canvas.showSafeFrame),
                        )
                        add(
                            ContextIconAction(Lucide.Grid2x2, "Grid", {
                                onOverflowMenuToggle(false)
                                onGridSettingsClick()
                            }),
                        )
                        add(
                            ContextIconAction(Lucide.Ghost, "Onion skin", {
                                onOverflowMenuToggle(false)
                                onOnionSettingsClick()
                            }),
                        )
                        onImportFrameImage?.let { import ->
                            add(
                                ContextIconAction(Icons.Filled.AddPhotoAlternate, "Frame image", {
                                    onOverflowMenuToggle(false)
                                    import()
                                }),
                            )
                        }
                    },
                )
                EditorAssetMenu(
                    assetsExpanded, { assetsExpanded = false }, state.project.backgroundAssetId != null,
                    onSetBackgroundClick, { onIntent(TimelineIntent.Background.Remove) },
                    onAddBackgroundToLibraryClick, onAddCurrentFrameToLibraryClick,
                    state.canvas.backgroundVisible, { onIntent(TimelineIntent.Background.ToggleVisibility) },
                )
            }
        }
    }
    if (speedOpen) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { speedOpen = false },
            title = { Text("Playback FPS") },
            text = { com.donnclab.kinetiframe.ui.components.timeline.TimelineFpsSlider(state.animation.fps, onIntent) },
            confirmButton = { ContextIconButton(ContextIconAction(Icons.Filled.Check, "Done", { speedOpen = false })) },
        )
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun EditorTopBarPreview() {
    EditorTopBar(
        TimelineState(), {}, {}, {}, {}, {},
        showOverflowMenu = false, onOverflowMenuToggle = {}, onFocusCanvas = {},
    )
}
