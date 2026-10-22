package com.donnclab.kinetiframe.common.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Sticky bottom action for the Home "Add" flows: an optional one-line summary of the current
 *  choices above a full-width primary button, elevated so it reads above a scrolled body. */
@Composable
fun AddFlowBottomBar(
    buttonLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    subtitle: String? = null,
) {
    Surface(shadowElevation = 4.dp, modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                )
            }
            Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(buttonLabel) }
        }
    }
}

@Preview
@Composable
private fun AddFlowBottomBarPreview() = AddFlowBottomBar(buttonLabel = "Create Project", onClick = {
}, subtitle = "Local · 1920 × 1080 · 12 fps")
