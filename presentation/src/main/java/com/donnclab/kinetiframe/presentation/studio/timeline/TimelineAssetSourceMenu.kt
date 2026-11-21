package com.donnclab.kinetiframe.presentation.studio.timeline

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.ContextIconAction
import com.donnclab.kinetiframe.presentation.common.IconContextMenu

/** What each source in [TimelineAssetSourceMenu] adds. */
data class AssetSourceActions(
    val onActor: () -> Unit,
    val onImportAudio: () -> Unit,
    val onRecordAudio: () -> Unit,
    val onVideo: () -> Unit,
    val onLibrary: () -> Unit,
)

/** Choose a media kind first; importing and recording are sources of Audio, not kinds. */
@Composable
fun TimelineAssetSourceMenu(expanded: Boolean, onDismiss: () -> Unit, actions: AssetSourceActions) {
    val (onActor, onImportAudio, onRecordAudio, onVideo, onLibrary) = actions
    var audio by remember(expanded) { mutableStateOf(false) }
    IconContextMenu(
        expanded && !audio,
        onDismiss,
        listOf(
            ContextIconAction(Icons.Default.TheaterComedy, stringResource(R.string.asset_source_actor), onActor),
            ContextIconAction(Icons.Default.Audiotrack, stringResource(R.string.asset_source_audio), { audio = true }),
            ContextIconAction(Icons.Default.VideoLibrary, stringResource(R.string.asset_source_video), onVideo),
            ContextIconAction(
                Icons.Default.CollectionsBookmark,
                stringResource(R.string.asset_source_library),
                onLibrary,
            ),
        ),
    )
    IconContextMenu(
        expanded && audio,
        onDismiss,
        listOf(
            ContextIconAction(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.asset_source_back), {
                audio =
                    false
            }),
            ContextIconAction(
                Icons.Default.FolderOpen,
                stringResource(R.string.asset_source_import_audio),
                onImportAudio,
            ),
            ContextIconAction(Icons.Default.Mic, stringResource(R.string.asset_source_record_audio), onRecordAudio),
        ),
    )
}

@Preview
@Composable
private fun TimelineAssetSourceMenuPreview() {
    TimelineAssetSourceMenu(true, {}, AssetSourceActions({}, {}, {}, {}, {}))
}
