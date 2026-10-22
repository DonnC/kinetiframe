package com.donnclab.kinetiframe.project.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.common.ui.AddFlowBottomBar
import com.donnclab.kinetiframe.common.ui.AddFlowTopBar
import com.donnclab.kinetiframe.common.ui.CompactTextField
import com.donnclab.kinetiframe.common.ui.StandardSlider

/**
 * Home's "New Project" flow: a name, the canvas size chosen on [AddProjectCanvasSizeScreen] (a
 * fixed FlipaClip-style export-preset list, never derived from this device's own screen), and an
 * FPS slider. [state] and [callbacks] are bundled per project convention so this composable
 * takes two values rather than a handful of separate lambdas.
 */
@Composable
fun AddProjectScreen(state: AddProjectFormState, callbacks: AddProjectCallbacks, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { AddFlowTopBar(title = "New Project", onBack = callbacks.onBack) },
        bottomBar = {
            AddFlowBottomBar(
                buttonLabel = "Create Project",
                enabled = state.name.isNotBlank(),
                onClick = callbacks.onCreate,
                subtitle = with(state.canvasSelection) {
                    "${preset.label} · ${dimensions.width} × ${dimensions.height} · ${state.fps} fps"
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            CompactTextField(
                value = state.name,
                onValueChange = {
                    callbacks.onStateChange(state.copy(name = it))
                },
                label = { Text("Project name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )

            Text(
                "Canvas size",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
            CanvasSizeRow(selection = state.canvasSelection, onClick = callbacks.onOpenCanvasSize)

            StandardSlider(
                value = state.fps.toFloat(),
                onValueChange = { callbacks.onStateChange(state.copy(fps = it.toInt())) },
                range = 1f..30f,
                steps = 28,
                label = "Frame rate",
                valueDisplay = { "${it.toInt()} FPS" },
                modifier = Modifier.padding(top = 28.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** The single nav row that opens [AddProjectCanvasSizeScreen], showing the current choice
 *  ("Local · 1920 × 1080") the same way FlipaClip's own project-setup screen does. */
@Composable
private fun CanvasSizeRow(selection: CanvasSizeSelection, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(selection.preset.label, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${selection.dimensions.width} × ${selection.dimensions.height}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Lucide.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(Modifier.padding(top = 0.dp))
}

@Preview
@Composable
private fun AddProjectScreenPreview() = AddProjectScreen(
    state = AddProjectFormState(name = "My Project"),
    callbacks = AddProjectCallbacks(onStateChange = {}, onOpenCanvasSize = {}, onBack = {}, onCreate = {}),
)
