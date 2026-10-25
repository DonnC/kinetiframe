package com.donnclab.kinetiframe.presentation.actors.sketch

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Lasso
import com.composables.icons.lucide.ListTree
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Text
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph

/** Choose a named part, then lasso its artwork. Reset is local to that part and undoable. */
@Composable
fun ActorSketchBindingMenu(state: ActorSketchState, graph: SceneGraph, onIntent: (ActorSketchIntent) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton({ open = true }, enabled = !state.busy) {
            Icon(
                if (state.tool == SketchTool.JOINT) Lucide.ListTree else Lucide.Lasso,
                if (state.tool ==
                    SketchTool.JOINT
                ) {
                    "Select drawing joint"
                } else {
                    "Correct drawing binding"
                },
                tint = if (state.tool ==
                    SketchTool.BIND_REGION
                ) {
                    MaterialTheme.colorScheme.primary
                } else {
                    LocalContentColor.current
                },
            )
        }
        DropdownMenu(open, { open = false }) {
            graph.bones.values.forEach { bone ->
                DropdownMenuItem(text = { Text(bone.name) }, onClick = {
                    onIntent(ActorSketchIntent.BindingPart(bone.id))
                    if (state.tool != SketchTool.JOINT) onIntent(ActorSketchIntent.Tool(SketchTool.BIND_REGION))
                    open = false
                })
            }
            state.bindingPartId?.let { part ->
                if (state.regions.any { it.boneId == part }) {
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Reset ${graph.bones[part]?.name ?: part} binding") }, onClick = {
                        onIntent(ActorSketchIntent.ClearRegions(part))
                        open = false
                    })
                }
            }
        }
    }
}

@Preview
@Composable
private fun SketchBindingPreview() {
    MaterialTheme {
        ActorSketchBindingMenu(
            ActorSketchState(busy = false),
            SceneGraph(
                listOf("root"),
                mapOf(
                    "root" to
                        BoneNode("root", name = "Body", length = 32f),
                ),
            ),
            {
            },
        )
    }
}
