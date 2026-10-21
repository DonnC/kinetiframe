package com.donnclab.kinetiframe.ui.color

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.donnclab.kinetiframe.data.actor.ActorFileStore
import com.donnclab.kinetiframe.data.color.BundledPalettes
import com.donnclab.kinetiframe.data.color.PaletteRepository
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import java.io.File

/** Same palette surface in the Library and every brush/fill colour popup. */
@Composable
fun PaletteBrowser(color: Color, onColorChanged: (Color) -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { PaletteRepository(ActorFileStore(File(context.filesDir, "actor-studio"))) }
    val model: PaletteViewModel = viewModel(
        key = "palette-browser",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = PaletteViewModel(repository) as T
        },
    )
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
    Column(Modifier.width(220.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            state.loading -> CircularProgressIndicator(Modifier.size(24.dp))

            state.error != null -> {
                Text(state.error, style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = { onIntent(PaletteIntent.Load) }) { Icon(Icons.Default.Refresh, "Retry palettes") }
            }

            else -> {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(state.palettes, key = { it.id }) { palette ->
                        FilterChip(
                            selected = state.selectedId == palette.id,
                            onClick = { onIntent(PaletteIntent.Select(palette.id)) },
                            label = { Text(palette.name, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                state.palettes.firstOrNull { it.id == state.selectedId }?.let { PaletteSwatches(it, color, onPick) }
                if (state.palettes.isEmpty()) Text("No palettes", style = MaterialTheme.typography.bodySmall)
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
