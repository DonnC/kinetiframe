// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.composables.icons.lucide.Clapperboard
import com.composables.icons.lucide.ClipboardPaste
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.Frame
import com.composables.icons.lucide.Ghost
import com.composables.icons.lucide.Grid2x2
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.ImagePlus
import com.composables.icons.lucide.ListVideo
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Maximize
import com.composables.icons.lucide.Minimize
import com.composables.icons.lucide.Redo
import com.composables.icons.lucide.Save
import com.composables.icons.lucide.Share
import com.composables.icons.lucide.Undo
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState

/** What the wide drawing bar's buttons do beyond sending studio intents. Null hides a button. */
data class WideBarActions(
    val onHome: (() -> Unit)?,
    val onFrames: () -> Unit,
    val onGridSettings: () -> Unit,
    val onTracks: (() -> Unit)?,
    val onSave: (() -> Unit)?,
    val onExport: (() -> Unit)?,
    val onBackground: () -> Unit = {},
    val onFocus: (() -> Unit)? = null,
    val onOnionSettings: () -> Unit = {},
    val onFrameImage: (() -> Unit)? = null,
    /** The frame reference and camera buttons, which need the editor's own state. */
    val viewControls: (@Composable () -> Unit)? = null,
)

/**
 * The drawing studio's top bar on a wide window: everyday actions in groups with labels, the
 * view toggles that live in the menu on a phone (Frames, Onion skin, Grid, Safe frame,
 * Tracks) out on the bar, and Save and Export at the end. A phone keeps the compact bar.
 */
@Composable
internal fun RowScope.WideDrawingBar(state: StudioState, onIntent: (StudioIntent) -> Unit, actions: WideBarActions) {
    actions.onHome?.let { WideBarButton(Icons.Filled.Home, stringResource(R.string.editor_home), onClick = it) }
    WideBarButton(Lucide.Undo, stringResource(R.string.editor_undo)) { onIntent(StudioIntent.Draw.Undo) }
    WideBarButton(Lucide.Redo, stringResource(R.string.editor_redo)) { onIntent(StudioIntent.Draw.Redo) }
    BarDivider()
    WideBarButton(Lucide.Copy, stringResource(R.string.editor_copy)) { onIntent(StudioIntent.Selection.Copy) }
    WideBarButton(Lucide.ClipboardPaste, stringResource(R.string.editor_paste)) {
        onIntent(StudioIntent.Selection.Paste)
    }
    FillViewButton(state, onIntent)
    Spacer(Modifier.weight(1f))
    WideBarButton(Lucide.Clapperboard, stringResource(R.string.workspace_frames), onClick = actions.onFrames)
    // Opens onion skin's settings, which hold its on/off switch, so nothing repeats in a menu.
    WideBarButton(
        Lucide.Ghost,
        stringResource(R.string.editor_onion_skin),
        selected = state.onionSkin.enabled,
        onClick = actions.onOnionSettings,
    )
    WideBarButton(
        Lucide.Grid2x2,
        stringResource(R.string.grid_title),
        selected = state.canvas.gridEnabled,
        onClick = actions.onGridSettings,
    )
    WideBarButton(Lucide.Frame, stringResource(R.string.editor_safe_frame), selected = state.canvas.showSafeFrame) {
        onIntent(StudioIntent.View.ToggleSafeFrame)
    }
    actions.onTracks?.let { WideBarButton(Lucide.ListVideo, stringResource(R.string.workspace_tracks), onClick = it) }
    WideBarButton(Lucide.Image, stringResource(R.string.editor_background), onClick = actions.onBackground)
    actions.onFrameImage?.let {
        WideBarButton(Lucide.ImagePlus, stringResource(R.string.workspace_frame_image), onClick = it)
    }
    actions.onFocus?.let { WideBarButton(Lucide.Eye, stringResource(R.string.workspace_focus), onClick = it) }
    actions.viewControls?.let { controls ->
        BarDivider()
        controls()
    }
    Spacer(Modifier.weight(1f))
    actions.onSave?.let { WideBarButton(Lucide.Save, stringResource(R.string.action_save), onClick = it) }
    // Export is a bar action like the rest, in the accent colour, so the bar fits a tablet's width.
    actions.onExport?.let {
        WideBarButton(Lucide.Share, stringResource(R.string.workspace_export), accent = true, onClick = it)
    }
}

/**
 * Fill and Fit: fills the view with the canvas, or fits the whole canvas once the view has moved.
 * Two quick two-finger taps on the canvas do the same.
 */
@Composable
private fun FillViewButton(state: StudioState, onIntent: (StudioIntent) -> Unit) {
    val fit = state.isCanvasTransformed
    WideBarButton(
        if (fit) Lucide.Minimize else Lucide.Maximize,
        stringResource(if (fit) R.string.editor_fit_view else R.string.editor_fill_view),
    ) { onIntent(StudioIntent.View.ToggleFillView) }
}

/**
 * An icon with its name under it; lit while [selected]. Controls placed on the wide bar from
 * elsewhere (the frame reference and camera buttons) use it too, so the bar reads as one row.
 * A [description] replaces the label for screen readers when it says more.
 */
@Composable
internal fun WideBarButton(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    accent: Boolean = false,
    description: String? = null,
    onClick: () -> Unit,
) {
    val tint = when {
        !enabled -> EditorTokens.inactive
        selected || accent -> EditorTokens.accent
        else -> EditorTokens.dark
    }
    Surface(
        onClick = onClick,
        modifier = if (description == null) Modifier else Modifier.semantics { contentDescription = description },
        enabled = enabled,
        shape = RoundedCornerShape(Dimens.radiusM),
        color = if (selected) EditorTokens.accent.copy(alpha = 0.12f) else Color.Transparent,
    ) {
        Column(
            Modifier.widthIn(
                min = EditorTokens.wideBarActionWidth,
            ).padding(horizontal = Dimens.spaceS, vertical = Dimens.spaceXxs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, null, Modifier.size(Dimens.iconM), tint = tint)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BarDivider() {
    VerticalDivider(Modifier.padding(horizontal = Dimens.spaceS, vertical = Dimens.spaceM))
}
