// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.sandbox.rigging

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.sandbox.R

@Composable
internal fun RiggingControls(state: SandboxState, onIntent: (SandboxIntent) -> Unit) {
    Row {
        RiggingPreviewToggle(state.previewMode) { onIntent(SandboxIntent.TogglePreview) }
        TextButton({ onIntent(SandboxIntent.Reset) }) { Text(stringResource(R.string.reset)) }
        TextButton({ onIntent(SandboxIntent.ToggleCapture) }) {
            Text(stringResource(if (state.capture.recording) R.string.stop_record else R.string.record))
        }
        TextButton({ onIntent(SandboxIntent.ToggleReplay) }, enabled = state.capture.targets.isNotEmpty()) {
            Text(stringResource(if (state.replay.playing) R.string.stop_replay else R.string.replay))
        }
    }
    Text(stringResource(R.string.sample_count, state.capture.targets.size, state.replay.frame + 1))
}

@Preview
@Composable
private fun RiggingControlsPreview() {
    MaterialTheme { RiggingControls(SandboxState(), {}) }
}
