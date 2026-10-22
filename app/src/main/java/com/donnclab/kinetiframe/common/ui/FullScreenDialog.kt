package com.donnclab.kinetiframe.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * A [content] composable presented as its own full screen rather than a small centered card --
 * used by the Home "Add" flows (new project, new pack, new actor), which are laid out like
 * dedicated screens (a back button, a scrollable body, a sticky bottom action) rather than an
 * [androidx.compose.material3.AlertDialog]. Implemented as a platform [Dialog] purely so the
 * system back gesture and button both map to [onDismiss] for free, without adding a NavKey
 * destination for what is really a short-lived, in-place flow off Home.
 */
@Composable
fun FullScreenDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            content()
        }
    }
}
