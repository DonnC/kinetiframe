package com.donnclab.kinetiframe.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.common.ui.BrandBadge
import com.donnclab.kinetiframe.common.ui.BrandWordmark
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

/** Consistent browse/create branding; tools remain contextual to the destination. */
@Composable
fun AppBrandBar(actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BrandBadge(Modifier.size(40.dp))
            BrandWordmark(Modifier.size(160.dp, 28.dp))
        }
        Row(content = actions)
    }
}

@Composable
fun ExitConfirmationDialog(onDismiss: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exit KinetiFrame?") },
        text = { Text("You can return to your saved projects whenever you're ready.") },
        confirmButton = { TextButton(onClick = onExit) { Text("Exit") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Stay") } },
    )
}

@Preview(showBackground = true)
@Composable
private fun AppBrandBarPreview() {
    KinetiFrameTheme { AppBrandBar() }
}

@Preview
@Composable
private fun ExitConfirmationPreview() {
    KinetiFrameTheme { ExitConfirmationDialog({}, {}) }
}
