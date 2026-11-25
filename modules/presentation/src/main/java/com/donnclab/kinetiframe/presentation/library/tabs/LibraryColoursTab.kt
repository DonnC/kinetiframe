// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.library.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
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
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.asString
import com.donnclab.kinetiframe.presentation.studio.color.PaletteIntent
import com.donnclab.kinetiframe.presentation.studio.color.PaletteState
import com.donnclab.kinetiframe.presentation.studio.color.PaletteViewModel
import com.donnclab.kinetiframe.ui.appViewModel

/**
 * Library's Colours tab, for keeping colours rather than drawing with them. My colours are the
 * quick colours under the studio's colour wheel. Palettes are named sets, such as one per character:
 * a New palette card starts one, and each palette opens to show, add, rename or remove its colours.
 * It shares its palettes with the studio's colour picker.
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
            item { Section(stringResource(R.string.palette_my_colours), stringResource(R.string.colours_mine_hint)) }
            item {
                val mine = state.myColours
                SwatchGrid(mine?.swatches.orEmpty(), { mine?.let { p -> shown = ShownColour(p, it, true) } }) {
                    sheet = ColourSheet.AddTo(null)
                }
            }
            item {
                Section(stringResource(R.string.color_palettes), stringResource(R.string.colours_palettes_hint))
            }
            item { NewPaletteCard { sheet = ColourSheet.NewPalette } }
            items(state.palettes.filterNot { state.isMyColours(it) }, key = { it.id }) { palette ->
                val editable = state.canEdit(palette)
                PaletteCard(
                    palette,
                    PaletteCardActions(
                        editable = editable,
                        onIntent = onIntent,
                        onColour = { shown = ShownColour(palette, it, editable) },
                        onAdd = { sheet = ColourSheet.AddTo(palette.id) },
                    ),
                )
            }
        }
    }
    sheet?.let { request ->
        ColourPickSheet(askName = request == ColourSheet.NewPalette, onDismiss = { sheet = null }) { name, colours ->
            sheet = null
            onIntent(
                when (request) {
                    ColourSheet.NewPalette -> PaletteIntent.Create(name, colours)
                    is ColourSheet.AddTo -> PaletteIntent.Add(colours.first(), request.paletteId)
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

/** A section's title and one line saying what it is for. */
@Composable
private fun Section(title: String, hint: String) {
    Column(Modifier.fillMaxWidth().padding(top = Dimens.spaceS)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Starts a palette, in the same outlined style as the add tile, so it reads as "make one here". */
@Composable
private fun NewPaletteCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(HomeTokens.cardCorner),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(Dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM),
        ) {
            Surface(
                shape = RoundedCornerShape(Dimens.radiusS),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(LibraryTokens.colourSwatch),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Lucide.Plus, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column {
                Text(stringResource(R.string.palette_new), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.palette_new_card_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun LibraryColoursPreview() {
    KinetiFrameTheme {
        LibraryColoursContent(PaletteState(false, BundledPalettes.entries, BundledPalettes.entries.first().id), {})
    }
}
