package com.donnclab.kinetiframe.presentation.actors.rigging

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/** Shared, bounded asset thumbnail for templates and mathematical presets. */
@Composable
fun ActorAssetCard(name: String, assetPath: String?, enabled: Boolean = true, onClick: () -> Unit) {
    Card(onClick = onClick, enabled = enabled, modifier = Modifier.width(144.dp)) {
        Column(Modifier.padding(8.dp)) {
            AsyncImage(
                model = assetPath?.let { "file:///android_asset/$it" },
                contentDescription = name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(104.dp),
            )
            Text(name, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Preview
@Composable
private fun ActorAssetCardPreview() {
    ActorAssetCard("Actor", null) {}
}
