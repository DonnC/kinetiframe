package com.donnclab.kinetiframe.presentation.studio.actorstudio.rigging

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Bone
import com.composables.icons.lucide.Layers
import com.composables.icons.lucide.Link2Off
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Move
import com.composables.icons.lucide.Pen
import com.composables.icons.lucide.Scissors
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.document.DrawingTool
import com.donnclab.kinetiframe.designsystem.theme.ActorStudioTokens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.actors.ActorNameDialog
import com.donnclab.kinetiframe.presentation.actors.rigging.ActorToolButton
import com.donnclab.kinetiframe.studio.ActorEditTarget
import com.donnclab.kinetiframe.studio.StudioIntent

/** Compact, icon-only rename/attach/detach actions for the part selected in [PartRail], shown as
 *  a small floating pill only while a part is selected so the canvas stays clear otherwise.
 *  Display-name changes never modify the structural part ID. */
@Composable
fun PartActionsRow(
    bone: BoneNode,
    setup: Boolean,
    onIntent: (StudioIntent) -> Unit,
    modifier: Modifier = Modifier,
    onChooseArtwork: (() -> Unit)? = null,
    onEditArtwork: ((String) -> Unit)? = null,
    onReplaceArtwork: ((String) -> Unit)? = null,
    editTarget: ActorEditTarget = ActorEditTarget.JOINT_END,
    canCaptureSketch: Boolean = false,
) {
    var rename by remember(bone.id) { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ActorStudioTokens.panelSurface,
        shadowElevation = 6.dp,
        modifier = modifier,
    ) {
        Row(Modifier.padding(4.dp)) {
            if (setup && bone.textureBinding != null) {
                ActorToolButton(
                    Lucide.Move,
                    stringResource(R.string.part_move_artwork),
                    selected = editTarget == ActorEditTarget.ARTWORK,
                ) {
                    onIntent(StudioIntent.Actor.SetEditTarget(ActorEditTarget.ARTWORK))
                }
            }
            ActorToolButton(
                Lucide.Bone,
                stringResource(R.string.part_move_joint),
                enabled = setup,
                selected = editTarget == ActorEditTarget.JOINT_START,
            ) {
                onIntent(StudioIntent.Actor.SetEditTarget(ActorEditTarget.JOINT_START))
            }
            if (setup && onChooseArtwork != null && onEditArtwork != null) {
                ActorArtworkControls(bone, onIntent, { id ->
                    onReplaceArtwork?.invoke(id) ?: onChooseArtwork()
                }, onEditArtwork)
            } else {
                onChooseArtwork?.let { choose ->
                    IconButton(onClick = choose, enabled = setup) {
                        Icon(
                            Icons.Default.Collections,
                            stringResource(R.string.part_attach_library_artwork),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            var stackMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = {
                    stackMenu = true
                }) { Icon(Lucide.Layers, stringResource(R.string.part_stacking), modifier = Modifier.size(18.dp)) }
                DropdownMenu(stackMenu, onDismissRequest = { stackMenu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.part_bring_forward)) }, onClick = {
                        onIntent(StudioIntent.Actor.MovePart(bone.id, true))
                        stackMenu =
                            false
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.part_send_backward)) }, onClick = {
                        onIntent(StudioIntent.Actor.MovePart(bone.id, false))
                        stackMenu =
                            false
                    })
                }
            }
            IconButton(onClick = { rename = true }, enabled = setup) {
                Icon(
                    Lucide.Pen,
                    stringResource(R.string.part_rename),
                    tint = if (setup) ActorStudioTokens.accent else ActorStudioTokens.onPanelMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
            if (setup &&
                canCaptureSketch
            ) {
                IconButton(onClick = { onIntent(StudioIntent.Draw.SelectTool(DrawingTool.LASSO_CUT)) }) {
                    Icon(
                        Lucide.Scissors,
                        stringResource(R.string.part_capture_sketch),
                        tint = ActorStudioTokens.accent,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            if (setup && bone.textureBinding != null) {
                IconButton(onClick = { onIntent(StudioIntent.Actor.Unbind) }) {
                    Icon(
                        Lucide.Link2Off,
                        stringResource(R.string.part_detach_artwork),
                        tint = ActorStudioTokens.accent,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
    if (rename) {
        ActorNameDialog(bone.name, stringResource(R.string.part_rename), { rename = false }) { name ->
            onIntent(StudioIntent.Actor.RenameBone(name))
            rename = false
        }
    }
}

@Preview
@Composable
private fun PartActionsRowPreview() {
    PartActionsRow(bone = BoneNode("torso", name = "Torso", length = 30f), setup = true, onIntent = {})
}
