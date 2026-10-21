package com.donnclab.kinetiframe.ui.components.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Captions add no toolbar chrome; accessibility and long-press tooltips remain available. */
@Composable
fun ContextMenuActionTile(action: ContextIconAction, showLabel: Boolean) {
    Column(Modifier.width(if (showLabel) 64.dp else 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ContextIconButton(action)
        if (showLabel) {
            Text(
                action.label,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LabelledActionPreview() = ContextMenuActionTile(
    ContextIconAction(Icons.Filled.ContentCopy, "Copy", {
    }),
    true,
)
