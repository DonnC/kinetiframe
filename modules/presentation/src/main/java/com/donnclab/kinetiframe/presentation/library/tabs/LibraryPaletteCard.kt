// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.library.tabs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.HomeTokens
import com.donnclab.kinetiframe.designsystem.theme.LibraryTokens
import com.donnclab.kinetiframe.library.model.ColorSwatch
import com.donnclab.kinetiframe.library.model.PaletteDocument
import com.donnclab.kinetiframe.library.model.PaletteEdits
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.studio.color.PaletteIntent

/** What a palette card can do; [editable] is false for the palettes that come with the app. */
internal data class PaletteCardActions(
    val editable: Boolean,
    val onIntent: (PaletteIntent) -> Unit,
    val onColour: (ColorSwatch) -> Unit,
    val onAdd: () -> Unit,
)

/**
 * One palette: its name, a strip of its colours, and a chevron that says it opens. Opened, it shows
 * every colour, then Rename and Delete for the user's own palette, or Copy to edit for one that
 * comes with the app.
 */
@Composable
internal fun PaletteCard(palette: PaletteDocument, actions: PaletteCardActions) {
    var open by rememberSaveable(palette.id) { mutableStateOf(false) }
    val turn by animateFloatAsState(if (open) HALF_TURN else 0f)
    val stateLabel = stringResource(if (open) R.string.palette_open_state else R.string.palette_closed_state)
    Surface(
        onClick = { open = !open },
        shape = RoundedCornerShape(HomeTokens.cardCorner),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().semantics { stateDescription = stateLabel },
    ) {
        Column(Modifier.padding(Dimens.spaceM), verticalArrangement = Arrangement.spacedBy(Dimens.spaceS)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    palette.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Default.ExpandMore,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(turn),
                )
            }
            if (open) {
                OpenPalette(palette, actions)
            } else {
                SwatchStrip(palette.swatches)
            }
        }
    }
}

@Composable
private fun OpenPalette(palette: PaletteDocument, actions: PaletteCardActions) {
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val roomLeft = palette.swatches.size < PaletteEdits.MAX_SWATCHES
    SwatchGrid(palette.swatches, actions.onColour, actions.onAdd.takeIf { actions.editable && roomLeft })
    if (actions.editable && !roomLeft) {
        Text(
            stringResource(R.string.palette_full, PaletteEdits.MAX_SWATCHES),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    val copyName = stringResource(R.string.palette_copy_name, palette.name)
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
        if (actions.editable) {
            CardAction(Icons.Default.Edit, stringResource(R.string.palette_rename)) { renaming = true }
            CardAction(Icons.Default.Delete, stringResource(R.string.palette_delete)) { confirmDelete = true }
        } else {
            CardAction(Icons.Default.ContentCopy, stringResource(R.string.palette_duplicate)) {
                actions.onIntent(PaletteIntent.Duplicate(palette.id, copyName))
            }
        }
    }
    if (renaming) {
        RenamePaletteDialog(
            current = palette.name,
            onRename = { name ->
                renaming = false
                actions.onIntent(PaletteIntent.Rename(palette.id, name))
            },
            onDismiss = { renaming = false },
        )
    }
    if (confirmDelete) {
        DeletePaletteDialog(
            onConfirm = {
                confirmDelete = false
                actions.onIntent(PaletteIntent.Delete(palette.id))
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun CardAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(icon, null, Modifier.size(Dimens.iconS))
        Text(label, modifier = Modifier.padding(start = Dimens.spaceXs))
    }
}

/** Colours as large, easy-to-tap tiles, ending with an add tile when [onAdd] is given. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun SwatchGrid(swatches: List<ColorSwatch>, onColour: (ColorSwatch) -> Unit, onAdd: (() -> Unit)?) {
    val shape = RoundedCornerShape(Dimens.radiusS)
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceS),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceS),
    ) {
        swatches.forEach { swatch ->
            val hex = PaletteEdits.hex(swatch.argb)
            Box(
                Modifier.size(LibraryTokens.colourSwatch)
                    .clip(shape)
                    .background(Color(swatch.argb.toInt()))
                    .border(Dimens.borderThin, MaterialTheme.colorScheme.outlineVariant, shape)
                    .semantics { contentDescription = hex }
                    .clickable { onColour(swatch) },
            )
        }
        onAdd?.let {
            Surface(
                onClick = it,
                shape = shape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.outline),
                modifier = Modifier.size(LibraryTokens.colourSwatch),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Lucide.Plus, stringResource(R.string.colours_add))
                }
            }
        }
    }
}

/** A palette at a glance: its colours side by side in one rounded bar. */
@Composable
private fun SwatchStrip(swatches: List<ColorSwatch>) {
    Row(
        Modifier.fillMaxWidth().height(LibraryTokens.colourStripHeight).clip(RoundedCornerShape(Dimens.radiusS)),
    ) {
        swatches.take(MAX_STRIP).forEach { swatch ->
            Box(Modifier.weight(1f).height(LibraryTokens.colourStripHeight).background(Color(swatch.argb.toInt())))
        }
    }
}

private const val MAX_STRIP = 16
private const val HALF_TURN = 180f
