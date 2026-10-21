// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.sandbox.rigging

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.sandbox.R

@Composable
internal fun RiggingScreen(state: SandboxState, onIntent: (SandboxIntent) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.rigging_title))
        Text(stringResource(R.string.rigging_hint))
        RiggingControls(state, onIntent)
        RiggingCanvas(state, onIntent, Modifier.weight(1f).fillMaxWidth())
    }
}

@Preview(widthDp = 640, heightDp = 360)
@Composable
private fun RiggingScreenPreview() {
    MaterialTheme { RiggingScreen(SandboxState(), {}) }
}
