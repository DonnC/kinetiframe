package com.donnclab.kinetiframe.presentation.studio.actorstudio.rigging

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.presentation.actors.rigging.ActorToolButton
import com.donnclab.kinetiframe.studio.StudioIntent

/** One compact entry covers the selected part's default and alternate artwork, for any rig. */
@Composable
fun ActorArtworkControls(
    bone: BoneNode,
    onIntent: (StudioIntent) -> Unit,
    onAttach: (String) -> Unit,
    onEdit: (String) -> Unit,
) {
    var open by remember(bone.id) { mutableStateOf(false) }
    var selected by remember(bone.id) { mutableStateOf("") }
    LaunchedEffect(bone.artworkVariants.keys) {
        if (selected.isNotEmpty() &&
            selected !in bone.artworkVariants
        ) {
            selected = ""
        }
    }
    val binding = if (selected.isEmpty()) bone.textureBinding else bone.artworkVariants[selected]?.binding
    Box {
        ActorToolButton(Icons.Default.Tune, "Edit part artwork") { open = true }
        DropdownMenu(open, { open = false }) {
            if (bone.artworkVariants.isNotEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            "${if (selected.isEmpty()) "✓ " else ""}Default artwork",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    onClick = { selected = "" },
                )
                bone.artworkVariants.forEach { (id, artwork) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                "${if (selected == id) "✓ " else ""}${artwork.name}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        onClick = { selected = id },
                    )
                }
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = {
                    Text(
                        if (binding ==
                            null
                        ) {
                            "Attach artwork"
                        } else {
                            "Replace artwork"
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                onClick = {
                    open = false
                    onAttach(selected)
                },
            )
            DropdownMenuItem(
                text = {
                    Text("Resize / align artwork", style = MaterialTheme.typography.labelSmall)
                },
                enabled =
                binding != null,
                onClick = {
                    open = false
                    onEdit(selected)
                },
            )
            if (bone.parentId != null && binding != null && binding.skin?.attachmentBlend != false) {
                DropdownMenuItem(
                    text = {
                        Text(
                            "${if (binding.skin?.attachmentBlend == true) "✓ " else ""}Blend attachment",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    onClick = {
                        open = false
                        onIntent(
                            StudioIntent.Actor.SetAttachmentBlend(
                                bone.id,
                                selected,
                                binding.skin?.attachmentBlend != true,
                            ),
                        )
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("Move artwork", style = MaterialTheme.typography.labelSmall) },
                enabled =
                bone.textureBinding != null,
                onClick = {
                    open = false
                    onIntent(StudioIntent.Rigging.SetStudioMode(com.donnclab.kinetiframe.studio.StudioMode.RIG_SETUP))
                    onIntent(StudioIntent.Actor.SetEditTarget(com.donnclab.kinetiframe.studio.ActorEditTarget.ARTWORK))
                },
            )
            DropdownMenuItem(
                text = { Text("Remove artwork", style = MaterialTheme.typography.labelSmall) },
                enabled =
                binding != null,
                onClick = {
                    open = false
                    onIntent(StudioIntent.Actor.RemoveArtwork(bone.id, selected))
                },
            )
        }
    }
}

@Preview
@Composable
private fun ArtworkControlsPreview() {
    MaterialTheme { ActorArtworkControls(BoneNode("part", name = "Part", length = 30f), {}, {}, {}) }
}
