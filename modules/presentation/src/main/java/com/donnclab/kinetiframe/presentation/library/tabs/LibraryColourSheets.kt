// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.library.tabs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.designsystem.theme.LibraryTokens
import com.donnclab.kinetiframe.library.model.ColorSwatch
import com.donnclab.kinetiframe.library.model.PaletteEdits
import com.donnclab.kinetiframe.presentation.R
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

/**
 * Picks a colour on a wheel or by hex and saves it with [onSave]. With [askName] it makes a new
 * palette instead: a name, then "Add this colour" for each colour it should hold, then Create.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ColourPickSheet(
    askName: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, colours: List<Long>) -> Unit,
) {
    var argb by remember { mutableLongStateOf(START_COLOUR) }
    var name by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf(emptyList<Long>()) }
    // A typed hex restarts the wheel there, so the slider then adjusts the typed colour.
    var wheelRestarts by remember { mutableIntStateOf(0) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.spaceXl).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceM),
        ) {
            Text(
                stringResource(if (askName) R.string.palette_new else R.string.colours_add),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            if (askName) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(PaletteEdits.MAX_NAME) },
                    label = { Text(stringResource(R.string.palette_name)) },
                    placeholder = { Text(stringResource(R.string.palette_name_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            val controller = androidx.compose.runtime.key(wheelRestarts) { rememberColorPickerController() }
            HsvColorPicker(
                modifier = Modifier.size(LibraryTokens.colourWheel),
                controller = controller,
                initialColor = remember(wheelRestarts) { Color(argb.toInt()) },
                onColorChanged = { if (it.fromUser) argb = it.color.toArgb().toLong() and ARGB_MASK },
            )
            BrightnessSlider(
                modifier = Modifier.width(LibraryTokens.colourWheel).height(EditorTokens.colorSliderHeight),
                controller = controller,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM),
            ) {
                ColourTile(argb, Modifier.size(LibraryTokens.colourSwatch))
                HexField(argb, Modifier.weight(1f)) {
                    argb = it
                    wheelRestarts++
                }
            }
            if (askName) {
                ChosenColours(chosen, onAdd = {
                    if (argb !in chosen && chosen.size < PaletteEdits.MAX_SWATCHES) chosen = chosen + argb
                }, onRemove = { gone -> chosen = chosen - gone })
            }
            Button(
                // A palette made without pressing "Add this colour" keeps the colour on the wheel.
                onClick = { onSave(name.trim(), if (askName) chosen.ifEmpty { listOf(argb) } else listOf(argb)) },
                enabled = !askName || name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.spaceL),
            ) { Text(stringResource(if (askName) R.string.palette_create else R.string.colours_save)) }
        }
    }
}

/** A new palette's colours so far, with "Add this colour" for the one on the wheel. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ChosenColours(chosen: List<Long>, onAdd: () -> Unit, onRemove: (Long) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.spaceS)) {
        OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.colours_add_this))
        }
        if (chosen.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceS),
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceS),
            ) {
                chosen.forEach { argb ->
                    val hex = PaletteEdits.hex(argb)
                    ColourTile(
                        argb,
                        Modifier.size(LibraryTokens.colourSwatch)
                            .semantics { contentDescription = hex }
                            .clickable { onRemove(argb) },
                    )
                }
            }
            Text(
                stringResource(R.string.colours_tap_to_take_out),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Renames one of the user's palettes. */
@Composable
internal fun RenamePaletteDialog(current: String, onRename: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.palette_rename_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(PaletteEdits.MAX_NAME) },
                label = { Text(stringResource(R.string.palette_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onRename(name.trim()) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.palette_rename))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** The colour as hex; typing a valid hex and pressing Done picks it. */
@Composable
private fun HexField(argb: Long, modifier: Modifier, onPick: (Long) -> Unit) {
    val current = PaletteEdits.hex(argb)
    var text by remember(current) { mutableStateOf(current) }
    val parsed = PaletteEdits.parseHex(text)
    OutlinedTextField(
        value = text,
        onValueChange = { text = it.take(HEX_LENGTH) },
        label = { Text(stringResource(R.string.palette_hex)) },
        singleLine = true,
        isError = parsed == null,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { parsed?.let(onPick) }),
        modifier = modifier,
    )
}

@Composable
private fun ColourTile(argb: Long, modifier: Modifier) {
    val shape = RoundedCornerShape(Dimens.radiusS)
    Box(
        modifier.clip(shape).background(Color(argb.toInt()))
            .border(Dimens.borderThin, MaterialTheme.colorScheme.outlineVariant, shape),
    )
}

/** One saved colour, large, with its hex to copy, and Remove when it is in the user's own palette. */
@Composable
internal fun ColourDetailDialog(swatch: ColorSwatch, onRemove: (() -> Unit)?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val hex = PaletteEdits.hex(swatch.argb)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(hex) },
        text = { ColourTile(swatch.argb, Modifier.fillMaxWidth().height(LibraryTokens.colourPreview)) },
        confirmButton = {
            TextButton(onClick = {
                context.copyText(hex)
                onDismiss()
            }) { Text(stringResource(R.string.colours_copy_hex)) }
        },
        dismissButton = {
            if (onRemove != null) {
                TextButton(onClick = {
                    onRemove()
                    onDismiss()
                }) { Text(stringResource(R.string.palette_remove_colour)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
            }
        },
    )
}

@Composable
internal fun DeletePaletteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.palette_delete_title)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.palette_delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private fun Context.copyText(text: String) {
    (
        getSystemService(
            Context.CLIPBOARD_SERVICE,
        ) as? ClipboardManager
        )?.setPrimaryClip(ClipData.newPlainText(text, text))
}

/** A mid red, so the wheel opens on a hue instead of black. */
private const val START_COLOUR = 0xFFE53935L
private const val ARGB_MASK = 0xFFFFFFFFL
private const val HEX_LENGTH = 9
