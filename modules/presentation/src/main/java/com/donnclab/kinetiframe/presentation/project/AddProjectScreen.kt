package com.donnclab.kinetiframe.presentation.project

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.AddFlowBottomBar
import com.donnclab.kinetiframe.presentation.common.AddFlowTopBar
import com.donnclab.kinetiframe.presentation.common.CompactTextField
import com.donnclab.kinetiframe.presentation.common.FrameRatePicker

/**
 * Home's "New Project" flow: a name, a canvas shape picked from picture cards (Landscape, Portrait,
 * Square or Custom, with every platform size one tap away in [CanvasSizeSheet]), and a frame rate
 * with a live preview. [state] and [callbacks] are bundled per project convention.
 */
@Composable
fun AddProjectScreen(state: AddProjectFormState, callbacks: AddProjectCallbacks, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { AddFlowTopBar(title = stringResource(R.string.add_project_title), onBack = callbacks.onBack) },
        bottomBar = {
            AddFlowBottomBar(
                buttonLabel = stringResource(R.string.add_project_create),
                enabled = state.name.isNotBlank(),
                onClick = callbacks.onCreate,
                subtitle = with(state.canvasSelection) {
                    stringResource(
                        R.string.add_project_summary,
                        preset.displayName(),
                        dimensions.width,
                        dimensions.height,
                        state.fps,
                    )
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.spaceXl),
        ) {
            CompactTextField(
                value = state.name,
                onValueChange = { callbacks.onStateChange(state.copy(name = it)) },
                label = { Text(stringResource(R.string.add_project_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.spaceXl),
            )
            SectionLabel(stringResource(R.string.add_project_canvas_size))
            CanvasShapeCards(state.canvasSelection) { preset ->
                callbacks.onStateChange(state.copy(canvasSelection = state.canvasSelection.copy(preset = preset)))
                // Custom opens the sheet, where its width and height are typed.
                if (preset.isCustom) callbacks.onOpenCanvasSize()
            }
            TextButton(onClick = callbacks.onOpenCanvasSize) { Text(stringResource(R.string.add_project_more_sizes)) }
            SectionLabel(stringResource(R.string.add_project_frame_rate))
            FrameRatePicker(state.fps, { callbacks.onStateChange(state.copy(fps = it)) })
            Spacer(Modifier.height(Dimens.spaceXlPlus))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Dimens.spaceXl, bottom = Dimens.spaceS),
    )
}

/** Landscape, Portrait, Square and Custom, each drawn at its real shape. A platform size picked from
 *  the sheet highlights none of them and shows its own name in the summary below. */
@Composable
private fun CanvasShapeCards(selection: CanvasSizeSelection, onPick: (CanvasPreset) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.spaceS)) {
        CanvasPresets.shapes.forEach { preset ->
            val selected = preset == selection.preset
            val size = if (preset.isCustom) selection.customDimensions else preset.dimensions
            Surface(
                onClick = { onPick(preset) },
                shape = RoundedCornerShape(Dimens.radiusM),
                color = if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                border = if (selected) BorderStroke(Dimens.borderMedium, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.weight(1f).semantics {
                    role = Role.RadioButton
                    this.selected = selected
                },
            ) {
                Column(
                    Modifier.padding(vertical = Dimens.spaceS),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
                ) {
                    Box(Modifier.size(Dimens.canvasShapeCard), contentAlignment = Alignment.Center) {
                        ShapeOutline(size.width, size.height, Dimens.canvasShapeMax, selected)
                    }
                    Text(preset.displayName(), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    Text(
                        stringResource(R.string.canvas_size_dimensions, size.width, size.height),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** A rectangle with the canvas's proportions, fitted inside [max] on its longer side. */
@Composable
private fun ShapeOutline(width: Int, height: Int, max: Dp, selected: Boolean) {
    val longest = maxOf(width, height).coerceAtLeast(1).toFloat()
    Surface(
        shape = RoundedCornerShape(Dimens.radiusXs),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            Dimens.borderMedium,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.size(max * (width / longest), max * (height / longest)),
    ) {}
}

@Preview
@Composable
private fun AddProjectScreenPreview() = AddProjectScreen(
    state = AddProjectFormState(name = "My Project"),
    callbacks = AddProjectCallbacks(onStateChange = {}, onOpenCanvasSize = {}, onBack = {}, onCreate = {}),
)
