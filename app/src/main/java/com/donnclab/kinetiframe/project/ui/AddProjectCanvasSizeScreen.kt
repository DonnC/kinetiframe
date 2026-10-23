package com.donnclab.kinetiframe.project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Link2
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.common.ui.AddFlowTopBar
import com.donnclab.kinetiframe.common.ui.CompactTextField
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.studio.ui.dialogs.SelectionDot

/**
 * The full named-preset list behind New Project's "Canvas size" row: the exact FlipaClip export
 * targets (see [CanvasPresets]), plus the editable [CanvasPresets.Local] size at the top. Tapping
 * a preset selects it immediately (no separate confirm step) -- the back arrow is the only way
 * out, matching how the rest of this screen family works.
 */
@Composable
fun AddProjectCanvasSizeScreen(
    selection: CanvasSizeSelection,
    onChange: (CanvasSizeSelection) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier, topBar = { AddFlowTopBar(title = "Canvas Size", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            LocalDimensionsRow(
                dimensions = selection.dimensions,
                editable = selection.preset.isLocal,
                onChange = { onChange(selection.copy(localDimensions = it)) },
            )
            HorizontalDivider(Modifier.padding(top = 8.dp, bottom = 4.dp))
            CanvasPresets.all.forEach { preset ->
                CanvasPresetRow(
                    preset = preset,
                    selected = preset == selection.preset,
                    onClick = { onChange(selection.copy(preset = preset)) },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** The width/height pair at the top, with a link glyph standing in for FlipaClip's aspect-lock
 *  affordance -- purely illustrative here (this screen doesn't yet re-derive one field from the
 *  other), since the width/height values themselves matter far more than that convenience. Only
 *  editable while [editable] (i.e. the [CanvasPresets.Local] preset is selected); every named
 *  preset's size is fixed. */
@Composable
private fun LocalDimensionsRow(dimensions: StageDimensions, editable: Boolean, onChange: (StageDimensions) -> Unit) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(top = 16.dp),
    ) {
        CompactTextField(
            value = dimensions.width.toString(),
            onValueChange = { text -> text.toIntOrNull()?.let { onChange(dimensions.copy(width = it)) } },
            label = { Text("Width") },
            suffix = { Text("px") },
            singleLine = true,
            enabled = editable,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        Icon(
            Lucide.Link2,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 14.dp),
        )
        CompactTextField(
            value = dimensions.height.toString(),
            onValueChange = { text -> text.toIntOrNull()?.let { onChange(dimensions.copy(height = it)) } },
            label = { Text("Height") },
            suffix = { Text("px") },
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
private fun CanvasPresetRow(preset: CanvasPreset, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 14.dp)) {
            Column(Modifier.weight(1f)) {
                Text(preset.label, style = MaterialTheme.typography.titleSmall)
                Text(
                    if (preset.isLocal) {
                        "${preset.dimensions.width} × ${preset.dimensions.height} · editable"
                    } else {
                        "${preset.dimensions.width} × ${preset.dimensions.height}"
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
private fun AddProjectCanvasSizeScreenPreview() =
    AddProjectCanvasSizeScreen(selection = CanvasSizeSelection(), onChange = {
    }, onBack = {})
