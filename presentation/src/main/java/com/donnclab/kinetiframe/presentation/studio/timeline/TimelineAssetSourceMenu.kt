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
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.presentation.common.ContextIconAction
import com.donnclab.kinetiframe.presentation.common.IconContextMenu

/** Choose a media kind first; importing and recording are sources of Audio, not kinds. */
@Composable
fun TimelineAssetSourceMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onActor: () -> Unit,
    onImportAudio: () -> Unit,
    onRecordAudio: () -> Unit,
    onVideo: () -> Unit,
    onLibrary: () -> Unit,
) {
    var audio by remember(expanded) { mutableStateOf(false) }
    IconContextMenu(
        expanded && !audio,
        onDismiss,
        listOf(
            ContextIconAction(Icons.Default.TheaterComedy, "Actor performance", onActor),
            ContextIconAction(Icons.Default.Audiotrack, "Audio", { audio = true }),
            ContextIconAction(Icons.Default.VideoLibrary, "Video", onVideo),
            ContextIconAction(Icons.Default.CollectionsBookmark, "Library", onLibrary),
        ),
    )
    IconContextMenu(
        expanded && audio,
        onDismiss,
        listOf(
            ContextIconAction(Icons.AutoMirrored.Filled.ArrowBack, "Back to assets", { audio = false }),
            ContextIconAction(Icons.Default.FolderOpen, "Import audio from device", onImportAudio),
            ContextIconAction(Icons.Default.Mic, "Record audio", onRecordAudio),
        ),
    )
}

@Preview
@Composable
private fun TimelineAssetSourceMenuPreview() {
    TimelineAssetSourceMenu(true, {}, {}, {}, {}, {}, {})
}
