package com.donnclab.kinetiframe.actors.ui.sketch

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.composables.icons.lucide.Box
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Layers
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Move
import com.composables.icons.lucide.Text
import com.donnclab.kinetiframe.actors.ui.rigging.ActorArtworkControls
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.studio.ActorEditTarget
import com.donnclab.kinetiframe.studio.StudioIntent

/** Retained cutouts are opt-in above the sketch. Erase drawn duplicates in the source editor. */
@Composable
fun ActorSketchArtworkControls(
    bone: BoneNode,
    enabled: Boolean,
    onIntent: (StudioIntent) -> Unit,
    onAttach: (String) -> Unit,
    onEdit: (String) -> Unit,
    behind: Boolean = false,
) {
    var layerMenu by remember { mutableStateOf(false) }
    IconButton(
        { onIntent(StudioIntent.Actor.SetDrawingOverlay(bone.id, !enabled, behind)) },
        enabled = bone.textureBinding != null || bone.artworkVariants.isNotEmpty(),
    ) {
        Icon(
            if (enabled) Lucide.Eye else Lucide.EyeOff,
            if (enabled) "Hide attached artwork" else "Show attached artwork",
        )
    }
    if (enabled) {
        androidx.compose.foundation.layout.Box {
            IconButton({ layerMenu = true }) { Icon(Lucide.Layers, "Artwork above or behind sketch") }
            DropdownMenu(layerMenu, { layerMenu = false }) {
                DropdownMenuItem(text = { Text("${if (!behind) "✓ " else ""}Above sketch") }, onClick = {
                    onIntent(StudioIntent.Actor.SetDrawingOverlay(bone.id, true))
                    layerMenu =
                        false
                })
                DropdownMenuItem(text = { Text("${if (behind) "✓ " else ""}Behind sketch") }, onClick = {
                    onIntent(StudioIntent.Actor.SetDrawingOverlay(bone.id, true, true))
                    layerMenu =
                        false
                })
            }
        }
    }
    if (enabled &&
        bone.textureBinding != null
    ) {
        IconButton({ onIntent(StudioIntent.Actor.SetEditTarget(ActorEditTarget.ARTWORK)) }) {
            Icon(Lucide.Move, "Move artwork above sketch")
        }
    }
    ActorArtworkControls(bone, onIntent, onAttach, onEdit)
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun SketchArtworkPreview() {
    MaterialTheme {
        androidx.compose.foundation.layout.Row {
            ActorSketchArtworkControls(BoneNode("eyes", name = "Eyes", length = 1f), false, {}, {}, {})
        }
    }
}
