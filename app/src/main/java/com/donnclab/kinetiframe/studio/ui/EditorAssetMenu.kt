package com.donnclab.kinetiframe.studio.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.common.ui.ContextIconAction
import com.donnclab.kinetiframe.common.ui.IconContextMenu

/** Persistent background actions belong together, not mixed with canvas display preferences. */
@Composable
fun EditorAssetMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    hasBackground: Boolean,
    onChoose: () -> Unit,
    onRemove: () -> Unit,
    onSaveBackground: (() -> Unit)?,
    onSaveFrame: (() -> Unit)?,
    backgroundVisible: Boolean = true,
    onToggleBackground: () -> Unit = {},
) {
    val actions = buildList {
        add(
            ContextIconAction(Icons.Filled.Image, if (hasBackground) "Replace background" else "Add background", {
                onDismiss()
                onChoose()
            }),
        )
        if (hasBackground) {
            add(
                ContextIconAction(
                    if (backgroundVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    if (backgroundVisible) "Hide background" else "Show background",
                    {
                        onDismiss()
                        onToggleBackground()
                    },
                    selected = !backgroundVisible,
                ),
            )
            add(
                ContextIconAction(Icons.Filled.DeleteOutline, "Remove background", {
                    onDismiss()
                    onRemove()
                }),
            )
            onSaveBackground?.let { save ->
                add(
                    ContextIconAction(Icons.Filled.Wallpaper, "Save background to Library", {
                        onDismiss()
                        save()
                    }),
                )
            }
        }
        onSaveFrame?.let { save ->
            add(
                ContextIconAction(Icons.Filled.PhotoLibrary, "Save frame to Library", {
                    onDismiss()
                    save()
                }),
            )
        }
    }
    IconContextMenu(expanded, onDismiss, actions)
}

@Preview(showBackground = true)
@Composable
private fun BackgroundMenuPreview() {
    MaterialTheme { EditorAssetMenu(true, {}, true, {}, {}, null, null) }
}
