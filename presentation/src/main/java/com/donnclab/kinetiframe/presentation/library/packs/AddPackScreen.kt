package com.donnclab.kinetiframe.presentation.library.packs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.presentation.common.AddFlowBottomBar
import com.donnclab.kinetiframe.presentation.common.AddFlowTopBar
import com.donnclab.kinetiframe.presentation.common.CompactTextField
import com.donnclab.kinetiframe.presentation.studio.dialogs.SelectionDot

/**
 * Home's "New Pack" flow: a name plus a square canvas size -- collected together up front, unlike
 * [PackCreatorRoute]'s older size-first-then-name-at-save-time flow, so a pack creator sees and
 * commits to both choices before drawing ever starts. Presented full-screen via
 * [com.donnclab.kinetiframe.presentation.common.FullScreenDialog], matching the New Project / New Actor
 * screens.
 */
@Composable
fun AddPackScreen(state: AddPackFormState, callbacks: AddPackCallbacks, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { AddFlowTopBar(title = "New Pack", onBack = callbacks.onBack) },
        bottomBar = {
            AddFlowBottomBar(
                buttonLabel = "Create Pack",
                enabled = state.name.isNotBlank(),
                onClick = callbacks.onCreate,
                subtitle = "${state.dimension} × ${state.dimension}",
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            CompactTextField(
                value = state.name,
                onValueChange = callbacks.onNameChange,
                label = { Text("Pack name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
            Text(
                "Pack canvas size",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PACK_SIZES.forEach { (size, tag) ->
                    PackSizeCard(
                        size = size,
                        tag = tag,
                        selected = size == state.dimension,
                        onClick = { callbacks.onDimensionChange(size) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** The three square sizes New Pack offers, each paired with its S/M/L shorthand. */
private val PACK_SIZES = listOf(512 to "S", 768 to "M", 1024 to "L")

/** One selectable square-size option: its S/M/L shorthand, its exact pixel size, and a
 *  [SelectionDot]. */
@Composable
private fun PackSizeCard(
    size: Int,
    tag: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.aspectRatio(1f),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.5.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { SelectionDot(selected) }
            Spacer(Modifier.height(12.dp))
            Text(tag, style = MaterialTheme.typography.headlineSmall)
            Text(
                "$size × $size",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun AddPackScreenPreview() = AddPackScreen(
    state = AddPackFormState(name = "My Pack"),
    callbacks = AddPackCallbacks(onNameChange = {}, onDimensionChange = {}, onBack = {}, onCreate = {}),
)
