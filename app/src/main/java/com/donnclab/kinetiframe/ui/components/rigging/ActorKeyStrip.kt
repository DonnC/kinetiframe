package com.donnclab.kinetiframe.ui.components.rigging

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.TimelineState
import com.donnclab.kinetiframe.ui.activeActor

/** Exact key selection avoids guessing a slider position to remove a key. */
@Composable
fun ActorKeyStrip(state: TimelineState, onIntent: (TimelineIntent) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        state.activeActor()?.poseKeys.orEmpty().forEach { key ->
            TextButton(onClick = { onIntent(TimelineIntent.ActorMotion.Scrub(key.timeMicros)) }) {
                Text("◆ ${"%.2f".format(key.timeMicros / 1_000_000.0)}s", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Preview
@Composable
private fun ActorKeyStripPreview() {
    ActorKeyStrip(TimelineState(), {})
}
