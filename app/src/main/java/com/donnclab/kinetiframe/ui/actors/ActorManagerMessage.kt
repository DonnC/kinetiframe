package com.donnclab.kinetiframe.ui.actors

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ActorManagerMessage(message: String, action: String, onAction: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(message, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = onAction) { Text(action) }
        }
    }
}

@Preview
@Composable
private fun ActorManagerMessagePreview() = ActorManagerMessage("No actors yet", "New actor", {})
