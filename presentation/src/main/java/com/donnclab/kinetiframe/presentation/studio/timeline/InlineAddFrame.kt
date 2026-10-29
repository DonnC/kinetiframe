package com.donnclab.kinetiframe.presentation.studio.timeline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** A pinned blank frame cell, not another standalone toolbar action. */
@Composable
fun InlineAddFrame(onClick: () -> Unit) {
    Box(Modifier.size(40.dp, 48.dp), contentAlignment = Alignment.Center) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(36.dp, 40.dp),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, "Add Frame", Modifier.size(18.dp)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InlineAddFramePreview() {
    InlineAddFrame({})
}
