package com.donnclab.kinetiframe.actors.ui.rigging

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.Focus
import com.composables.icons.lucide.ListTree
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Paintbrush
import com.composables.icons.lucide.Save
import com.donnclab.kinetiframe.common.ui.ContextIconAction
import com.donnclab.kinetiframe.common.ui.IconContextMenu
import com.donnclab.kinetiframe.studio.ActorEditTarget
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioMode

@Composable
fun PuppetTopBar(
    onIntent: (StudioIntent) -> Unit,
    onInspectorToggle: () -> Unit,
    studioMode: StudioMode = StudioMode.RIG_SETUP,
    skeletonOverlayVisible: Boolean = true,
    performanceOnly: Boolean = false,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onSave: (() -> Unit)? = null,
    onSavePerformance: (() -> Unit)? = null,
    onPerformanceActions: (() -> Unit)? = null,
    onExit: (() -> Unit)? = null,
    actorViews: (@Composable () -> Unit)? = null,
    onAttachArtwork: ((Boolean) -> Unit)? = null,
    hasSelectedPart: Boolean = true,
) {
    Surface {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compact = maxWidth < 480.dp
            var more by remember { mutableStateOf(false) }
            Row(
                Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActorToolButton(Icons.AutoMirrored.Filled.ArrowBack, "Back from Actor Studio") {
                    onExit?.invoke() ?: onIntent(StudioIntent.Rigging.SetStudioMode(StudioMode.RASTER_DRAWING))
                }
                if (!performanceOnly) {
                    StudioModePill(current = studioMode, onSelect = {
                        onIntent(StudioIntent.Rigging.SetStudioMode(it))
                    })
                } else {
                    Icon(Icons.AutoMirrored.Filled.DirectionsRun, "Performance", modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.weight(1f))
                actorViews?.invoke()
                if (!performanceOnly && studioMode != StudioMode.ANIMATE_IK) {
                    onAttachArtwork?.let { choose ->
                        ActorPackAttachmentButton(choose, enabled = hasSelectedPart)
                    }
                }
                if (!performanceOnly &&
                    !compact
                ) {
                    ActorToolButton(
                        Lucide.Bone,
                        "Edit skeleton joints",
                        selected =
                        studioMode == StudioMode.RIG_SETUP,
                    ) {
                        onIntent(StudioIntent.Rigging.SetStudioMode(StudioMode.RIG_SETUP))
                        onIntent(StudioIntent.Actor.SetEditTarget(ActorEditTarget.JOINT_END))
                    }
                }
                if (!compact) {
                    ActorToolButton(Icons.AutoMirrored.Filled.Undo, "Undo actor edit", canUndo) {
                        onIntent(StudioIntent.Actor.Undo)
                    }
                    ActorToolButton(Icons.AutoMirrored.Filled.Redo, "Redo actor edit", canRedo) {
                        onIntent(StudioIntent.Actor.Redo)
                    }
                }
                ActorToolButton(
                    Lucide.ListTree,
                    if (studioMode ==
                        StudioMode.ANIMATE_IK
                    ) {
                        "Motion presets"
                    } else {
                        "Character hierarchy"
                    },
                    onClick = onInspectorToggle,
                )
                onSave?.let { save -> ActorToolButton(Lucide.Save, "Save actor revision", onClick = save) }
                onSavePerformance?.let { save -> ActorToolButton(Lucide.Save, "Save performance", onClick = save) }
                Box {
                    ActorToolButton(Lucide.EllipsisVertical, "More actor tools") { more = true }
                    IconContextMenu(
                        more,
                        { more = false },
                        buildList {
                            if (compact) {
                                if (!performanceOnly) {
                                    add(
                                        ContextIconAction(Lucide.Bone, "Edit skeleton joints", {
                                            onIntent(StudioIntent.Rigging.SetStudioMode(StudioMode.RIG_SETUP))
                                            onIntent(StudioIntent.Actor.SetEditTarget(ActorEditTarget.JOINT_END))
                                            more = false
                                        }),
                                    )
                                }
                                add(
                                    ContextIconAction(Icons.AutoMirrored.Filled.Undo, "Undo actor edit", {
                                        onIntent(StudioIntent.Actor.Undo)
                                        more =
                                            false
                                    }, enabled = canUndo),
                                )
                                add(
                                    ContextIconAction(Icons.AutoMirrored.Filled.Redo, "Redo actor edit", {
                                        onIntent(StudioIntent.Actor.Redo)
                                        more =
                                            false
                                    }, enabled = canRedo),
                                )
                            }
                            add(
                                ContextIconAction(Lucide.Bone, "Skeleton overlay", {
                                    onIntent(StudioIntent.Rigging.ToggleSkeletonOverlay)
                                    more =
                                        false
                                }, selected = skeletonOverlayVisible),
                            )
                            add(
                                ContextIconAction(Lucide.Focus, "Fit puppet canvas", {
                                    onIntent(StudioIntent.View.ResetCanvasTransform)
                                    more =
                                        false
                                }),
                            )
                            onPerformanceActions?.let { action ->
                                add(
                                    ContextIconAction(Lucide.EllipsisVertical, "Performance actions", {
                                        more =
                                            false
                                        action()
                                    }),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 640)
@Composable
private fun PuppetTopBarPreview() {
    PuppetTopBar({}, {})
}
