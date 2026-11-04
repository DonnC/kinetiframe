package com.donnclab.kinetiframe.presentation.project

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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.AddFlowBottomBar
import com.donnclab.kinetiframe.presentation.common.AddFlowTopBar
import com.donnclab.kinetiframe.presentation.common.CompactTextField
import com.donnclab.kinetiframe.presentation.common.StandardSlider

/**
 * Home's "New Project" flow: a name, the canvas size chosen on [AddProjectCanvasSizeScreen] (a
 * fixed FlipaClip-style export-preset list, never derived from this device's own screen), and an
 * FPS slider. [state] and [callbacks] are bundled per project convention so this composable
 * takes two values rather than a handful of separate lambdas.
 */
@Composable
fun AddProjectScreen(state: AddProjectFormState, callbacks: AddProjectCallbacks, modifier: Modifier = Modifier) {
    val resources = LocalResources.current
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
                        preset.label,
                        dimensions.width,
                        dimensions.height,
                        state.fps,
                    )
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
                label = { Text(stringResource(R.string.add_project_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )

            Text(
                stringResource(R.string.add_project_canvas_size),
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
                label = stringResource(R.string.add_project_frame_rate),
                valueDisplay = { resources.getString(R.string.add_project_fps_value, it.toInt()) },
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
                    stringResource(
                        R.string.canvas_size_dimensions,
                        selection.dimensions.width,
                        selection.dimensions.height,
                    ),
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
