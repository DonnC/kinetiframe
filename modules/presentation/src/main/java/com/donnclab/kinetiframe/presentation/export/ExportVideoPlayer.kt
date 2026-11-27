// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.export

import android.widget.VideoView
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import com.donnclab.kinetiframe.presentation.R
import java.io.File

/** Whether the export preview is playing the finished video, and how to start and stop it. */
internal data class PreviewPlayback(val playing: Boolean, val onPlay: () -> Unit, val onStop: () -> Unit)

/**
 * Plays an exported video once inside the export preview, using the platform's player so no other
 * app opens. Tapping it stops; reaching the end or failing to play returns to the Play button.
 */
@Composable
internal fun ExportVideoPlayer(file: File, onStop: () -> Unit, modifier: Modifier = Modifier) {
    val stop by rememberUpdatedState(onStop)
    val description = stringResource(R.string.export_stop_watching)
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                setOnCompletionListener { stop() }
                setOnErrorListener { _, _, _ ->
                    stop()
                    true
                }
                setVideoPath(file.absolutePath)
                start()
            }
        },
        onRelease = VideoView::stopPlayback,
        modifier = modifier.semantics { contentDescription = description }.clickable { stop() },
    )
}
