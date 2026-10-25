package com.donnclab.kinetiframe.actors.ui.rigging

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/** Universal attachment entry: parts can belong to any actor structure, not only a human. */
@Composable
fun ActorPackAttachmentButton(onChoose: (asVariant: Boolean) -> Unit, enabled: Boolean = true) {
    ActorToolButton(Icons.Default.Collections, "Attach artwork", enabled) { onChoose(false) }
}

@Preview
@Composable
private fun ActorPackAttachmentPreview() {
    MaterialTheme { ActorPackAttachmentButton({}) }
}
