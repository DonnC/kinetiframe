package com.donnclab.kinetiframe.presentation.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Link2
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.CompactTextField
import com.donnclab.kinetiframe.presentation.studio.dialogs.SelectionDot

/**
 * New Project's canvas sizes in a scrollable bottom sheet: the editable [CanvasPresets.Custom] size
 * at the top, then the named presets (see [CanvasPresets]). Picking a named preset selects it and
 * closes the sheet; the editable size keeps it open while the width and height are typed.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CanvasSizeSheet(selection: CanvasSizeSelection, onChange: (CanvasSizeSelection) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        // Clear of the navigation bar, so the last sizes aren't under it (R3-33).
        LazyColumn(
            Modifier.fillMaxWidth().imePadding().navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = Dimens.spaceXl),
        ) {
            item {
                Text(stringResource(R.string.canvas_size_title), style = MaterialTheme.typography.titleLarge)
                LocalDimensionsRow(
                    dimensions = selection.dimensions,
                    editable = selection.preset.isCustom,
                    onChange = { onChange(selection.copy(customDimensions = it)) },
                )
                HorizontalDivider(Modifier.padding(top = Dimens.spaceS, bottom = Dimens.spaceXs))
            }
            items(CanvasPresets.all) { preset ->
                CanvasPresetRow(
                    preset = preset,
                    selected = preset == selection.preset,
                    // Custom's row shows the size it would use: the one in the fields above.
                    shown = if (preset.isCustom) selection.dimensions else preset.dimensions,
                    onClick = {
                        onChange(selection.choose(preset))
                        if (!preset.isCustom) onDismiss()
                    },
                )
            }
            item { Spacer(Modifier.height(Dimens.spaceXlPlus)) }
        }
    }
}

/** The width/height pair at the top, with a link glyph standing in for FlipaClip's aspect-lock
 *  affordance -- purely illustrative here (this screen doesn't yet re-derive one field from the
 *  other), since the width/height values themselves matter far more than that convenience. Only
 *  editable while [editable] (i.e. the [CanvasPresets.Custom] preset is selected); every named
 *  preset's size is fixed. */
@Composable
private fun LocalDimensionsRow(dimensions: StageDimensions, editable: Boolean, onChange: (StageDimensions) -> Unit) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM),
        modifier = Modifier.padding(top = Dimens.spaceL),
    ) {
        CompactTextField(
            value = dimensions.width.toString(),
            onValueChange = { text -> text.toIntOrNull()?.let { onChange(dimensions.copy(width = it)) } },
            label = { Text(stringResource(R.string.canvas_size_width)) },
            suffix = { Text(stringResource(R.string.canvas_size_px)) },
            singleLine = true,
            enabled = editable,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        Icon(
            Lucide.Link2,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Dimens.spaceMPlus),
        )
        CompactTextField(
            value = dimensions.height.toString(),
            onValueChange = { text -> text.toIntOrNull()?.let { onChange(dimensions.copy(height = it)) } },
            label = { Text(stringResource(R.string.canvas_size_height)) },
            suffix = { Text(stringResource(R.string.canvas_size_px)) },
            singleLine = true,
            enabled = editable,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
    }
}

/** One row in the preset list: name, its fixed pixel size (or "editable" for Local), and a
 *  selection indicator. */
@Composable
private fun CanvasPresetRow(preset: CanvasPreset, selected: Boolean, shown: StageDimensions, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = Dimens.spaceMPlus)) {
            Column(Modifier.weight(1f)) {
                Text(preset.displayName(), style = MaterialTheme.typography.titleSmall)
                Text(
                    if (preset.isCustom) {
                        stringResource(R.string.canvas_size_editable, shown.width, shown.height)
                    } else {
                        stringResource(R.string.canvas_size_dimensions, shown.width, shown.height)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SelectionDot(selected)
        }
    }
}

@Preview
@Composable
private fun CanvasSizeSheetPreview() = CanvasSizeSheet(selection = CanvasSizeSelection(), onChange = {}, onDismiss = {})
