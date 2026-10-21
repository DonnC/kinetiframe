// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.sandbox.rigging

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.sandbox.R

@Composable
internal fun RiggingPreviewToggle(mode: RigPreviewMode, onToggle: () -> Unit) {
    val description = stringResource(
        when (mode) {
            RigPreviewMode.BONES -> R.string.preview_mesh
            RigPreviewMode.SKIN_MESH -> R.string.preview_bones
        },
    )
    val color = MaterialTheme.colorScheme.primary
    IconButton(onToggle, Modifier.semantics { contentDescription = description }) {
        Canvas(Modifier.size(24.dp)) {
            for (index in 0..2) {
                val fraction = index / 2f
                drawLine(color, Offset(size.width * fraction, 0f), Offset(size.width * fraction, size.height), 2f)
                drawLine(color, Offset(0f, size.height * fraction), Offset(size.width, size.height * fraction), 2f)
            }
        }
    }
}

@Preview
@Composable
private fun PreviewToggle() {
    MaterialTheme { RiggingPreviewToggle(RigPreviewMode.BONES, {}) }
}
