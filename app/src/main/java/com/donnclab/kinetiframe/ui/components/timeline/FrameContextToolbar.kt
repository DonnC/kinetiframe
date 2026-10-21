package com.donnclab.kinetiframe.ui.components.timeline

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Link
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.ClipboardPaste
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.SquarePlus
import com.composables.icons.lucide.Trash2
import com.donnclab.kinetiframe.ui.TimelineIntent

/** Single-frame actions for the compact editor timeline -- built on the same [ContextActionBar]
 *  every other selectable timeline item (audio/video clips, actor placements) uses. */
@Composable
fun FrameContextToolbar(
    index: Int,
    onIntent: (TimelineIntent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onEditDetails: (() -> Unit)? = null,
    onHeldDrawing: (() -> Unit)? = null,
) {
    ContextActionBar(
        modifier = modifier,
        actions = listOf(
            ContextAction(
                Lucide.ClipboardPaste,
                "Paste before",
                badge = androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowBack,
                badgeAlignment = Alignment.TopStart,
            ) {
                onIntent(TimelineIntent.Frame.PasteFrame(index, before = true))
                onDismiss()
            },
            ContextAction(
                Lucide.SquarePlus,
                "Add before",
                badge = androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowBack,
                badgeAlignment = Alignment.TopStart,
            ) {
                onIntent(TimelineIntent.Frame.AddFrameAt(index))
                onDismiss()
            },
            ContextAction(Lucide.Trash2, "Delete") {
                onIntent(TimelineIntent.Frame.DeleteFrame(index))
                onDismiss()
            },
            ContextAction(Lucide.Copy, "Copy") {
                onIntent(TimelineIntent.Frame.CopyFrame(index))
                onDismiss()
            },
            ContextAction(
                Lucide.SquarePlus,
                "Add after",
                badge = androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowForward,
                badgeAlignment = Alignment.TopEnd,
            ) {
                onIntent(TimelineIntent.Frame.AddFrameAt(index + 1))
                onDismiss()
            },
            ContextAction(
                Lucide.ClipboardPaste,
                "Paste after",
                badge = androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowForward,
                badgeAlignment = Alignment.TopEnd,
            ) {
                onIntent(TimelineIntent.Frame.PasteFrame(index, before = false))
                onDismiss()
            },
        ) + listOfNotNull(
            onHeldDrawing?.let { held ->
                ContextAction(Icons.Default.Link, "Held drawing") {
                    held()
                    onDismiss()
                }
            },
            onEditDetails?.let { edit ->
                ContextAction(Lucide.Pencil, "Edit item details") {
                    edit()
                    onDismiss()
                }
            },
        ),
    )
}
