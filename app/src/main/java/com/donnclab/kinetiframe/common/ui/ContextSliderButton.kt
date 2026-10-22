package com.donnclab.kinetiframe.common.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** A secondary inspector only when requested. Commit once on release, not on every sample. */
@Composable
fun ContextSliderButton(
    icon: ImageVector,
    label: String,
    value: Float,
    onCommit: (Float) -> Unit,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onPreview: (Float) -> Unit = {},
) {
    var open by remember { mutableStateOf(false) }
    var draft by remember(value, open) { mutableFloatStateOf(value) }
    Box {
        ContextIconButton(ContextIconAction(icon, label, { open = true }, enabled))
        DropdownMenu(open, { open = false }) {
            Slider(
                draft,
                {
                    draft = it
                    onPreview(it)
                },
                enabled = enabled,
                valueRange = valueRange,
                onValueChangeFinished = { onCommit(draft) },
                modifier = Modifier.width(192.dp).padding(horizontal = 12.dp)
                    .semantics { contentDescription = label },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContextSliderPreview() {
    MaterialTheme { ContextSliderButton(Icons.AutoMirrored.Filled.VolumeUp, "Volume", .7f, {}) }
}
