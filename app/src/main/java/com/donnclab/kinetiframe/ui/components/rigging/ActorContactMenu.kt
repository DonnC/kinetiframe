package com.donnclab.kinetiframe.ui.components.rigging

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview

/** Contact is explicit intent, never guessed from a name or a frame's apparent ground height. */
@Composable
fun ActorContactMenu(available: Boolean, pinned: Boolean, onToggle: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ActorToolButton(Icons.Default.PushPin, "Joint contact", enabled = available, selected = pinned) {
            expanded =
                true
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(if (pinned) "Release joint from this key" else "Pin joint from this key") },
                onClick = {
                    expanded = false
                    onToggle()
                },
            )
        }
    }
}

@Preview
@Composable
private fun ActorContactMenuPreview() {
    ActorContactMenu(true, false, {})
}
