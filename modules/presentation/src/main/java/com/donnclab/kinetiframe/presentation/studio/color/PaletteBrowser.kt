package com.donnclab.kinetiframe.presentation.studio.color

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.library.model.BundledPalettes
import com.donnclab.kinetiframe.library.model.PaletteDocument
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
                var naming by remember { mutableStateOf(false) }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
                    item {
                        AssistChip(
                            onClick = { naming = true },
                            label = {
                                Text(stringResource(R.string.palette_new), style = MaterialTheme.typography.labelSmall)
                            },
                            leadingIcon = { Icon(Lucide.Plus, null, Modifier.size(Dimens.iconS)) },
                        )
                    }
                    items(state.palettes, key = { it.id }) { palette ->
                        FilterChip(
                            selected = state.selectedId == palette.id,
                            onClick = { onIntent(PaletteIntent.Select(palette.id)) },
                            label = { Text(palette.displayName(state), style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                state.palettes.firstOrNull { it.id == state.selectedId }?.let { palette ->
                    val editable = state.canEdit(palette)
                    PaletteSwatches(
                        palette,
                        color,
                        onPick,
                        onRemove = if (editable) {
                            { swatch -> onIntent(PaletteIntent.Remove(palette.id, swatch.name)) }
                        } else {
                            null
                        },
                    )
                    if (editable) EditablePaletteActions(palette.id, color, onIntent)
                }
                if (naming) {
                    NewPaletteDialog(
                        onCreate = { name ->
                            naming = false
                            onIntent(PaletteIntent.Create(name, listOf(color.toArgb().toLong() and 0xFFFFFFFFL)))
                        },
                        onDismiss = { naming = false },
                    )
                }
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

/** "Add current colour" and "Delete palette", for a palette the user made. */
@Composable
private fun EditablePaletteActions(paletteId: String, color: Color, onIntent: (PaletteIntent) -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
        TextButton(onClick = { onIntent(PaletteIntent.Add(color.toArgb().toLong() and 0xFFFFFFFFL, paletteId)) }) {
            Icon(Lucide.Plus, null, Modifier.size(Dimens.iconS))
            Text(stringResource(R.string.palette_save_colour))
        }
        TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.palette_delete)) }
    }
    Text(stringResource(R.string.palette_edit_hint), style = MaterialTheme.typography.bodySmall)
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.palette_delete_title)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onIntent(PaletteIntent.Delete(paletteId))
                }) { Text(stringResource(R.string.palette_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun NewPaletteDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.palette_new)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(MAX_NAME) },
                label = { Text(stringResource(R.string.palette_name)) },
                supportingText = { Text(stringResource(R.string.palette_new_hint)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name.trim()) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.palette_create))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private const val MAX_NAME = 60

/** My colours shows in the user's language; other palettes show the name they were given. */
@Composable
private fun PaletteDocument.displayName(state: PaletteState): String =
    if (state.isMyColours(this)) stringResource(R.string.palette_my_colours) else name

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
