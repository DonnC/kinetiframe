package com.donnclab.kinetiframe.presentation.studio.color

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.library.model.BundledPalettes
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.asString
import com.donnclab.kinetiframe.ui.appViewModel

/** Same palette surface in the Library and every brush/fill colour popup. */
@Composable
fun PaletteBrowser(color: Color, onColorChanged: (Color) -> Unit) {
    val model: PaletteViewModel = appViewModel(key = "palette-browser")
    val state by model.state.collectAsStateWithLifecycle()
    PaletteBrowserContent(state, color, model::handleIntent, onColorChanged)
}

@Composable
fun PaletteBrowserContent(
    state: PaletteState,
    color: Color,
    onIntent: (PaletteIntent) -> Unit,
    onPick: (Color) -> Unit,
) {
    Column(Modifier.width(EditorTokens.palettePanelWidth), verticalArrangement = Arrangement.spacedBy(Dimens.spaceS)) {
        when {
            state.loading -> CircularProgressIndicator(Modifier.size(Dimens.iconL))

            state.error != null -> {
                Text(state.error.asString(), style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = {
                    onIntent(PaletteIntent.Load)
                }) { Icon(Icons.Default.Refresh, stringResource(R.string.palette_retry)) }
            }

            else -> {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
                    items(state.palettes, key = { it.id }) { palette ->
                        FilterChip(
                            selected = state.selectedId == palette.id,
                            onClick = { onIntent(PaletteIntent.Select(palette.id)) },
                            label = { Text(palette.name, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                state.palettes.firstOrNull { it.id == state.selectedId }?.let { PaletteSwatches(it, color, onPick) }
                if (state.palettes.isEmpty()) {
                    Text(
                        stringResource(R.string.palette_empty),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PaletteBrowserPreview() {
    KinetiFrameTheme {
        PaletteBrowserContent(
            PaletteState(
                false,
                BundledPalettes.entries,
                BundledPalettes.entries.first().id,
            ),
            Color.Black,
            {},
            {},
        )
    }
}
