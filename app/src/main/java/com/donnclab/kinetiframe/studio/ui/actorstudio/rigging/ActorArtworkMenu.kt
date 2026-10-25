package com.donnclab.kinetiframe.studio.ui.actorstudio.rigging

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.presentation.actors.rigging.ActorToolButton
import com.donnclab.kinetiframe.studio.StudioIntent

/** Variant choices key only this part at the selected performance timestamp. */
@Composable
fun ActorArtworkMenu(bone: BoneNode, onIntent: (StudioIntent) -> Unit) {
    var open by remember(bone.id) { mutableStateOf(false) }
    Box {
        ActorToolButton(Icons.Default.Collections, "Part artwork", bone.artworkVariants.isNotEmpty()) { open = true }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem(text = { Text("Default", style = MaterialTheme.typography.labelSmall) }, onClick = {
                open = false
                onIntent(StudioIntent.Actor.SelectArtwork(bone.id, ""))
            })
            bone.artworkVariants.forEach { (id, variant) ->
                DropdownMenuItem(text = { Text(variant.name, style = MaterialTheme.typography.labelSmall) }, onClick = {
                    open = false
                    onIntent(StudioIntent.Actor.SelectArtwork(bone.id, id))
                })
            }
        }
    }
}

@Preview
@Composable
private fun ArtworkMenuPreview() {
    MaterialTheme { ActorArtworkMenu(BoneNode("mouth", name = "Mouth", length = 20f), {}) }
}
