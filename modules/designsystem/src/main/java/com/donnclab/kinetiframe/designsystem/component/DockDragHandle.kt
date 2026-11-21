package com.donnclab.kinetiframe.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.GripHorizontal
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

@Composable
fun DockDragHandle(modifier: Modifier = Modifier) {
    Box(modifier.padding(vertical = 8.dp).size(20.dp), contentAlignment = Alignment.Center) {
        Icon(Lucide.GripHorizontal, contentDescription = null, Modifier.size(16.dp), tint = EditorTokens.inactive)
    }
}

@Preview(showBackground = true)
@Composable
private fun DockDragHandlePreview() {
    KinetiFrameTheme { DockDragHandle() }
}
