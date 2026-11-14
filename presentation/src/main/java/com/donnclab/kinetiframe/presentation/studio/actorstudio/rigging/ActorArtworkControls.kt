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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.actors.rigging.ActorToolButton
import com.donnclab.kinetiframe.presentation.common.checkedLabel
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
        ActorToolButton(Icons.Default.Tune, stringResource(R.string.part_artwork_edit)) { open = true }
        DropdownMenu(open, { open = false }) {
            if (bone.artworkVariants.isNotEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            checkedLabel(
                                stringResource(R.string.part_artwork_default_full),
                                checked = selected.isEmpty(),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    onClick = { selected = "" },
                )
                bone.artworkVariants.forEach { (id, artwork) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                checkedLabel(artwork.name, checked = selected == id),
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
                        stringResource(
                            if (binding ==
                                null
                            ) {
                                R.string.part_artwork_attach
                            } else {
                                R.string.part_artwork_replace
                            },
                        ),
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
                    Text(stringResource(R.string.part_artwork_resize), style = MaterialTheme.typography.labelSmall)
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
                            checkedLabel(
                                stringResource(R.string.part_artwork_blend),
                                checked = binding.skin?.attachmentBlend == true,
                            ),
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
                text = {
                    Text(stringResource(R.string.part_artwork_move), style = MaterialTheme.typography.labelSmall)
                },
                enabled =
                bone.textureBinding != null,
                onClick = {
                    open = false
                    onIntent(StudioIntent.Rigging.SetStudioMode(com.donnclab.kinetiframe.studio.StudioMode.RIG_SETUP))
                    onIntent(StudioIntent.Actor.SetEditTarget(com.donnclab.kinetiframe.studio.ActorEditTarget.ARTWORK))
                },
            )
            DropdownMenuItem(
                text = {
                    Text(stringResource(R.string.part_artwork_remove), style = MaterialTheme.typography.labelSmall)
                },
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
