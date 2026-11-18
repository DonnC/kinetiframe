package com.donnclab.kinetiframe.presentation.studio

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.ContextIconAction
import com.donnclab.kinetiframe.presentation.common.IconContextMenu

/** What [EditorAssetMenu]'s items do. A null save action hides its item. */
data class BackgroundMenuActions(
    val onChoose: () -> Unit,
    val onRemove: () -> Unit,
    val onSaveBackground: (() -> Unit)? = null,
    val onSaveFrame: (() -> Unit)? = null,
    val onToggleBackground: () -> Unit = {},
)

/** Persistent background actions belong together, not mixed with canvas display preferences. */
@Composable
fun EditorAssetMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    hasBackground: Boolean,
    menuActions: BackgroundMenuActions,
    backgroundVisible: Boolean = true,
) {
    val (onChoose, onRemove, onSaveBackground, onSaveFrame, onToggleBackground) = menuActions
    val actions = buildList {
        add(
            ContextIconAction(
                Icons.Filled.Image,
                stringResource(
                    if (hasBackground) R.string.editor_background_replace else R.string.editor_background_add,
                ),
                {
                    onDismiss()
                    onChoose()
                },
            ),
        )
        if (hasBackground) {
            add(
                ContextIconAction(
                    if (backgroundVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    stringResource(
                        if (backgroundVisible) R.string.editor_background_hide else R.string.editor_background_show,
                    ),
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
    MaterialTheme { EditorAssetMenu(true, {}, true, BackgroundMenuActions({}, {})) }
}
