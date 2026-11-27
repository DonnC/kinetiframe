package com.donnclab.kinetiframe.presentation.studio.timeline

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.studio.StudioIntent

/**
 * An explicit editing workspace: opening lanes must never remeasure the drawing viewport.
 *
 * In portrait it opens at half height over the canvas, with a handle to drag it taller, shorter or
 * closed. In landscape, where height is scarce, it opens full height.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TracksWorkspaceDialog(
    isPlaying: Boolean,
    onIntent: (StudioIntent) -> Unit,
    onDismiss: () -> Unit,
    assetPicker: (@Composable () -> Unit)? = null,
    /** The tablet workspace docks the lanes under the canvas instead (see [TracksDockedPanel]). */
    docked: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (docked) {
        TracksDockedPanel(onDismiss, assetPicker, content)
        return
    }
    val portrait = LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = !portrait),
        modifier = Modifier.fillMaxSize(),
        sheetMaxWidth = androidx.compose.ui.unit.Dp.Unspecified,
        dragHandle = if (portrait) ({ BottomSheetDefaults.DragHandle() }) else null,
        contentWindowInsets = { WindowInsets.safeDrawing },
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.tracks_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f).padding(start = Dimens.spaceL),
                )
                PlaybackControls(isPlaying, onIntent)
                assetPicker?.invoke()
                IconButton(onClick = onDismiss) { Icon(Lucide.X, stringResource(R.string.tracks_close)) }
            }
            content()
        }
    }
}

/**
 * The tablet workspace's tracks: a panel docked under the canvas instead of a sheet over the
 * whole studio, so the canvas stays in view while lanes are edited.
 */
@Composable
fun TracksDockedPanel(
    onDismiss: () -> Unit,
    assetPicker: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        // As tall as its lanes need, up to a limit, so an empty panel stays small.
        modifier = Modifier.fillMaxWidth().heightIn(max = EditorTokens.workspaceTracksHeight)
            .padding(horizontal = Dimens.spaceS),
        shape = RoundedCornerShape(Dimens.radiusM),
        tonalElevation = Dimens.elevationRaised,
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.tracks_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f).padding(start = Dimens.spaceL),
                )
                // No transport here: the frame strip's sits right below (expert review).
                assetPicker?.invoke()
                IconButton(onClick = onDismiss) { Icon(Lucide.X, stringResource(R.string.tracks_close)) }
            }
            // The lanes scroll themselves within the height this box gives them.
            Box(Modifier.weight(1f, fill = false)) { content() }
        }
    }
}

@Preview(widthDp = 800, heightDp = 360)
@Composable
private fun TracksWorkspacePreview() {
    TracksWorkspaceDialog(false, {}, {}) { Text("Track workspace", Modifier.padding(Dimens.spaceL)) }
}
