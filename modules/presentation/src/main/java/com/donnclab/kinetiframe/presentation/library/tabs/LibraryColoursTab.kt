// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.library.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.HomeTokens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.designsystem.theme.LibraryTokens
import com.donnclab.kinetiframe.library.model.BundledPalettes
import com.donnclab.kinetiframe.library.model.ColorSwatch
import com.donnclab.kinetiframe.library.model.PaletteDocument
import com.donnclab.kinetiframe.library.model.PaletteEdits
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.ContextIconAction
import com.donnclab.kinetiframe.presentation.common.MediaCardMenu
import com.donnclab.kinetiframe.presentation.common.asString
import com.donnclab.kinetiframe.presentation.studio.color.PaletteIntent
import com.donnclab.kinetiframe.presentation.studio.color.PaletteState
import com.donnclab.kinetiframe.presentation.studio.color.PaletteViewModel
import com.donnclab.kinetiframe.ui.appViewModel

/**
 * Library's Colours tab, for keeping colours rather than drawing with them: My colours as a grid
 * with an add tile, then every palette as a card with a strip of its colours. Tapping a palette opens
 * it; tapping a colour shows its hex. It shares its palettes with the studio's colour picker.
 */
@Composable
fun LibraryColoursTab() {
    val model: PaletteViewModel = appViewModel(key = "palette-browser")
    val state by model.state.collectAsStateWithLifecycle()
    LibraryColoursContent(state, model::handleIntent)
}

@Composable
internal fun LibraryColoursContent(state: PaletteState, onIntent: (PaletteIntent) -> Unit) {
    var sheet by remember { mutableStateOf<ColourSheet?>(null) }
    var shown by remember { mutableStateOf<ShownColour?>(null) }
    when {
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        state.error != null -> Column(
            Modifier.fillMaxSize().padding(Dimens.spaceXl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(state.error.asString(), style = MaterialTheme.typography.bodyMedium)
            IconButton(onClick = { onIntent(PaletteIntent.Load) }) {
                Icon(Icons.Default.Refresh, stringResource(R.string.palette_retry))
            }
        }

        else -> LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Dimens.spaceXl,
                end = Dimens.spaceXl,
                top = Dimens.spaceM,
                bottom = HomeTokens.listBottomReserve,
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceM),
        ) {
            item { SectionTitle(stringResource(R.string.palette_my_colours)) }
            item {
                val mine = state.myColours
                if (mine == null || mine.swatches.isEmpty()) {
                    Text(
                        stringResource(R.string.colours_mine_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SwatchGrid(mine?.swatches.orEmpty(), { mine?.let { p -> shown = ShownColour(p, it, true) } }) {
                    sheet = ColourSheet.AddTo(null)
                }
            }
            item {
                SectionTitle(stringResource(R.string.color_palettes)) {
                    TextButton(onClick = { sheet = ColourSheet.NewPalette }) {
                        Icon(Lucide.Plus, null, Modifier.size(Dimens.iconS))
                        Text(stringResource(R.string.palette_new))
                    }
                }
            }
            val palettes = state.palettes.filterNot { state.isMyColours(it) }
            if (palettes.isEmpty()) item { Text(stringResource(R.string.palette_empty)) }
            items(palettes, key = { it.id }) { palette ->
                val editable = state.canEdit(palette)
                PaletteCard(
                    palette = palette,
                    editable = editable,
                    onDelete = { onIntent(PaletteIntent.Delete(palette.id)) },
                    onColour = { shown = ShownColour(palette, it, editable) },
                    onAdd = { sheet = ColourSheet.AddTo(palette.id) },
                )
            }
        }
    }
    sheet?.let { request ->
        ColourPickSheet(askName = request == ColourSheet.NewPalette, onDismiss = { sheet = null }) { name, argb ->
            sheet = null
            onIntent(
                when (request) {
                    ColourSheet.NewPalette -> PaletteIntent.Create(name, argb)
                    is ColourSheet.AddTo -> PaletteIntent.Add(argb, request.paletteId)
                },
            )
        }
    }
    shown?.let { colour ->
        ColourDetailDialog(
            swatch = colour.swatch,
            onRemove = if (colour.editable) {
                { onIntent(PaletteIntent.Remove(colour.palette.id, colour.swatch.name)) }
            } else {
                null
            },
            onDismiss = { shown = null },
        )
    }
}

/** What the add sheet is open for: a colour for a palette (My colours when null), or a new palette. */
private sealed interface ColourSheet {
    data class AddTo(val paletteId: String?) : ColourSheet
    data object NewPalette : ColourSheet
}

private data class ShownColour(val palette: PaletteDocument, val swatch: ColorSwatch, val editable: Boolean)

@Composable
private fun SectionTitle(text: String, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = Dimens.spaceS), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

/** Colours as large, easy-to-tap tiles, ending with an add tile when [onAdd] is given. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SwatchGrid(swatches: List<ColorSwatch>, onColour: (ColorSwatch) -> Unit, onAdd: (() -> Unit)?) {
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
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
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

/** One palette: its name and size, a strip of its colours, and all of them once opened. */
@Composable
private fun PaletteCard(
    palette: PaletteDocument,
    editable: Boolean,
    onDelete: () -> Unit,
    onColour: (ColorSwatch) -> Unit,
    onAdd: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var open by rememberSaveable(palette.id) { mutableStateOf(false) }
    Surface(
        onClick = { open = !open },
        shape = RoundedCornerShape(HomeTokens.cardCorner),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = Dimens.spaceM, bottom = Dimens.spaceM, end = Dimens.spaceXs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(vertical = Dimens.spaceS)) {
                    Text(palette.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    val count = palette.swatches.size
                    val detail = pluralStringResource(R.plurals.colours_count, count, count)
                    Text(
                        if (editable) detail else "$detail · ${stringResource(R.string.colours_starter)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (editable) {
                    MediaCardMenu(
                        stringResource(R.string.colours_palette_actions),
                        listOf(
                            ContextIconAction(Lucide.Plus, stringResource(R.string.colours_add), onAdd),
                            ContextIconAction(Icons.Default.Delete, stringResource(R.string.palette_delete), {
                                confirmDelete = true
                            }),
                        ),
                    )
                }
            }
            Box(Modifier.padding(end = Dimens.spaceS)) {
                if (open) {
                    SwatchGrid(palette.swatches, onColour, if (editable) onAdd else null)
                } else {
                    SwatchStrip(palette.swatches)
                }
            }
        }
    }
    if (confirmDelete) {
        DeletePaletteDialog(
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
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

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun LibraryColoursPreview() {
    KinetiFrameTheme {
        LibraryColoursContent(PaletteState(false, BundledPalettes.entries, BundledPalettes.entries.first().id), {})
    }
}
