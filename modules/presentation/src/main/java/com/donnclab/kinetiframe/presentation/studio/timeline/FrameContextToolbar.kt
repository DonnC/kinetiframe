package com.donnclab.kinetiframe.presentation.studio.timeline

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Link
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.composables.icons.lucide.ClipboardPaste
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.CopyPlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.SquarePlus
import com.composables.icons.lucide.Trash2
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.studio.StudioIntent

/** Single-frame actions for the compact editor timeline -- built on the same [ContextActionBar]
 *  every other selectable timeline item (audio/video clips, actor placements) uses. */
@Composable
fun FrameContextToolbar(
    index: Int,
    onIntent: (StudioIntent) -> Unit,
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
                stringResource(R.string.frame_menu_paste_before),
                badge = androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowBack,
                badgeAlignment = Alignment.TopStart,
            ) {
                onIntent(StudioIntent.Frame.PasteFrame(index, before = true))
                onDismiss()
            },
            ContextAction(
                Lucide.SquarePlus,
                stringResource(R.string.frame_menu_add_before),
                badge = androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowBack,
                badgeAlignment = Alignment.TopStart,
            ) {
                onIntent(StudioIntent.Frame.AddFrameAt(index))
                onDismiss()
            },
            ContextAction(Lucide.Trash2, stringResource(R.string.frame_menu_delete)) {
                onIntent(StudioIntent.Frame.DeleteFrame(index))
                onDismiss()
            },
            // One tap to duplicate: Copy closes the menu, so copy-then-paste took four (R3-25).
            ContextAction(Lucide.CopyPlus, stringResource(R.string.frame_menu_duplicate)) {
                onIntent(StudioIntent.Frame.DuplicateFrame(index))
                onDismiss()
            },
            ContextAction(Lucide.Copy, stringResource(R.string.frame_menu_copy)) {
                onIntent(StudioIntent.Frame.CopyFrame(index))
                onDismiss()
            },
            ContextAction(
                Lucide.SquarePlus,
                stringResource(R.string.frame_menu_add_after),
                badge = androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowForward,
                badgeAlignment = Alignment.TopEnd,
            ) {
                onIntent(StudioIntent.Frame.AddFrameAt(index + 1))
                onDismiss()
            },
            ContextAction(
                Lucide.ClipboardPaste,
                stringResource(R.string.frame_menu_paste_after),
                badge = androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowForward,
                badgeAlignment = Alignment.TopEnd,
            ) {
                onIntent(StudioIntent.Frame.PasteFrame(index, before = false))
                onDismiss()
            },
        ) + listOfNotNull(
            onHeldDrawing?.let { held ->
                ContextAction(Icons.Default.Link, stringResource(R.string.frame_menu_held_drawing)) {
                    held()
                    onDismiss()
                }
            },
            onEditDetails?.let { edit ->
                ContextAction(Lucide.Pencil, stringResource(R.string.frame_menu_edit_details)) {
                    edit()
                    onDismiss()
                }
            },
        ),
    )
}
