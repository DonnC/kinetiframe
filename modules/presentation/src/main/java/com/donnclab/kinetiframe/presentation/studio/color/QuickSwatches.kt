// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.color

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.library.model.ColorSwatch
import com.donnclab.kinetiframe.library.model.PaletteEdits
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.library.tabs.ColourDetailDialog
import com.donnclab.kinetiframe.ui.appViewModel

/**
 * The colours an animator reaches for most, under the colour wheel: recent colours, their saved
 * colours with a "+" to save the current one, and a hex field. Long-press a saved colour to see its
 * hex or remove it, as in Library.
 */
@Composable
fun QuickSwatches(color: Color, onPick: (Color) -> Unit) {
    val model: PaletteViewModel = appViewModel(key = "palette-browser")
    val state by model.state.collectAsStateWithLifecycle()
    val latest by rememberUpdatedState(color)
    // The colour the picker closes on is the one drawn with next, so it becomes recent.
    DisposableEffect(model) { onDispose { model.handleIntent(PaletteIntent.Used(latest.argb())) } }
    Column(Modifier.width(EditorTokens.palettePanelWidth), verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
        if (state.recent.isNotEmpty()) {
            Text(stringResource(R.string.palette_recent), style = MaterialTheme.typography.labelSmall)
            SwatchRow(state.recent, color, onPick)
        }
        Text(stringResource(R.string.palette_my_colours), style = MaterialTheme.typography.labelSmall)
        val mine = state.myColours
        SwatchRow(
            mine?.swatches?.map { it.argb }.orEmpty(),
            color,
            onPick,
            onRemove = { argb ->
                mine?.swatches?.firstOrNull { it.argb == argb }?.let {
                    model.handleIntent(PaletteIntent.Remove(mine.id, it.name))
                }
            },
            leading = {
                FilledTonalIconButton(
                    onClick = { model.handleIntent(PaletteIntent.Add(color.argb())) },
                    modifier = Modifier.size(EditorTokens.paletteSwatch),
                ) { Icon(Lucide.Plus, stringResource(R.string.palette_save_colour)) }
            },
        )
        HexField(color, onPick)
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SwatchRow(
    colours: List<Long>,
    selected: Color,
    onPick: (Color) -> Unit,
    onRemove: ((Long) -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    var opened by remember { mutableStateOf<Long?>(null) }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
        leading?.let { item { it() } }
        items(colours, key = { it }) { argb ->
            val swatch = Color(argb.toInt())
            val isSelected = selected.argb() == argb
            val description = PaletteEdits.hex(argb)
            Box(
                Modifier.size(EditorTokens.paletteSwatch)
                    .background(swatch, RoundedCornerShape(Dimens.radiusXs))
                    .border(
                        if (isSelected) EditorTokens.swatchBorderSelected else Dimens.borderThin,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(Dimens.radiusXs),
                    )
                    .semantics { contentDescription = description }
                    .combinedClickable(
                        onClick = { onPick(swatch) },
                        onLongClickLabel = onRemove?.let { stringResource(R.string.palette_open_colour) },
                        onLongClick = onRemove?.let {
                            {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                opened = argb
                            }
                        },
                    ),
            )
        }
    }
    opened?.let { argb ->
        ColourDetailDialog(
            ColorSwatch(PaletteEdits.hex(argb), argb),
            onRemove?.let { remove -> { remove(argb) } },
            onDismiss = { opened = null },
        )
    }
}

/** The current colour as hex; typing a hex and pressing Done picks it. */
@Composable
private fun HexField(color: Color, onPick: (Color) -> Unit) {
    val current = PaletteEdits.hex(color.argb())
    var text by remember(current) { mutableStateOf(current) }
    val parsed = PaletteEdits.parseHex(text)
    OutlinedTextField(
        value = text,
        onValueChange = { text = it.take(HEX_LENGTH) },
        label = { Text(stringResource(R.string.palette_hex)) },
        singleLine = true,
        isError = parsed == null,
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { parsed?.let { onPick(Color(it.toInt())) } }),
        modifier = Modifier.width(EditorTokens.palettePanelWidth),
    )
}

private const val HEX_LENGTH = 9

private fun Color.argb(): Long = toArgb().toLong() and 0xFFFFFFFFL
