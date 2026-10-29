package com.donnclab.kinetiframe.presentation.studio.timeline

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens

@Composable
fun FrameActionIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(34.dp)) {
        IconButton(onClick = onClick, modifier = Modifier.size(30.dp)) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(17.dp), tint = EditorTokens.dark)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = EditorTokens.muted, maxLines = 1)
    }
}
