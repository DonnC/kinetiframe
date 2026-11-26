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
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.style.TextOverflow
import com.composables.icons.lucide.Clapperboard
import com.composables.icons.lucide.ClipboardPaste
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Frame
import com.composables.icons.lucide.Ghost
import com.composables.icons.lucide.Grid2x2
import com.composables.icons.lucide.ListVideo
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Maximize
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
    val onFrameRate: () -> Unit,
    val onTracks: (() -> Unit)?,
    val onSave: (() -> Unit)?,
    val onExport: (() -> Unit)?,
)

/**
 * The drawing studio's top bar on a wide window: everyday actions in groups with labels, the
 * view toggles that live in the menu on a phone (Frames, Onion skin, Grid, Safe frame, frame
 * rate, Tracks) out on the bar, and Save and Export at the end. A phone keeps the compact bar.
 */
@Composable
internal fun RowScope.WideDrawingBar(state: StudioState, onIntent: (StudioIntent) -> Unit, actions: WideBarActions) {
    actions.onHome?.let { BarAction(Icons.Filled.Home, stringResource(R.string.editor_home), onClick = it) }
    BarAction(Lucide.Undo, stringResource(R.string.editor_undo)) { onIntent(StudioIntent.Draw.Undo) }
    BarAction(Lucide.Redo, stringResource(R.string.editor_redo)) { onIntent(StudioIntent.Draw.Redo) }
    BarDivider()
    BarAction(Lucide.Copy, stringResource(R.string.editor_copy)) { onIntent(StudioIntent.Selection.Copy) }
    BarAction(Lucide.ClipboardPaste, stringResource(R.string.editor_paste)) { onIntent(StudioIntent.Selection.Paste) }
    BarAction(Lucide.Maximize, stringResource(R.string.editor_reset_view), enabled = state.isCanvasTransformed) {
        onIntent(StudioIntent.View.ResetCanvasTransform)
    }
    Spacer(Modifier.weight(1f))
    BarAction(Lucide.Clapperboard, stringResource(R.string.workspace_frames), onClick = actions.onFrames)
    BarAction(Lucide.Ghost, stringResource(R.string.editor_onion_skin), selected = state.onionSkin.enabled) {
        onIntent(StudioIntent.OnionSkin.SetEnabled(!state.onionSkin.enabled))
    }
    BarAction(
        Lucide.Grid2x2,
        stringResource(R.string.grid_title),
        selected = state.canvas.gridEnabled,
        onClick = actions.onGridSettings,
    )
    BarAction(Lucide.Frame, stringResource(R.string.editor_safe_frame), selected = state.canvas.showSafeFrame) {
        onIntent(StudioIntent.View.ToggleSafeFrame)
    }
    BarAction(Icons.Filled.Speed, stringResource(R.string.fps_chip, state.animation.fps), onClick = actions.onFrameRate)
    actions.onTracks?.let { BarAction(Lucide.ListVideo, stringResource(R.string.workspace_tracks), onClick = it) }
    Spacer(Modifier.weight(1f))
    actions.onSave?.let { BarAction(Lucide.Save, stringResource(R.string.action_save), onClick = it) }
    actions.onExport?.let { export ->
        Button(onClick = export, modifier = Modifier.padding(horizontal = Dimens.spaceS)) {
            Icon(Lucide.Share, null, Modifier.size(Dimens.iconS))
            Text(stringResource(R.string.editor_export_video), Modifier.padding(start = Dimens.spaceS))
        }
    }
}

/** An icon with its name under it; lit while [selected]. */
@Composable
private fun BarAction(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val tint = when {
        !enabled -> EditorTokens.inactive
        selected -> EditorTokens.accent
        else -> EditorTokens.dark
    }
    Surface(
        onClick = onClick,
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
