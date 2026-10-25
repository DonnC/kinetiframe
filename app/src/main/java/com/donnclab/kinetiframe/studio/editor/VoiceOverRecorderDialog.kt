// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.media.audio.VoiceRecorder
import com.donnclab.kinetiframe.presentation.media.audio.AudioRecorderActions
import com.donnclab.kinetiframe.presentation.media.audio.AudioRecorderSheet
import com.donnclab.kinetiframe.presentation.media.audio.PendingAudioTake
import kotlinx.coroutines.CoroutineScope
import java.io.File

/** Records a voice-over take and hands it to [onTake] for trimming. */
@Composable
internal fun VoiceOverRecorderDialog(scope: CoroutineScope, onClose: () -> Unit, onTake: (PendingAudioTake) -> Unit) {
    val context = LocalContext.current
    // The recorder owns a native MediaRecorder, so it lives exactly as long as this dialog.
    val voiceRecorder = remember { VoiceRecorder(context, scope) }
    val recorderState by voiceRecorder.state.collectAsStateWithLifecycle()
    val takeFile = remember { File(context.cacheDir, "voiceover-${System.currentTimeMillis()}.m4a") }
    DisposableEffect(Unit) { onDispose { voiceRecorder.release() } }

    val close = {
        voiceRecorder.release()
        onClose()
    }
    // The platform default dialog width is too narrow for the recorder's control row.
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AudioRecorderSheet(
            modifier = Modifier.widthIn(max = Dimens.recorderSheetMaxWidth).padding(horizontal = Dimens.spaceL),
            state = recorderState,
            actions = AudioRecorderActions(
                onRecord = { voiceRecorder.start(takeFile) },
                onStop = { voiceRecorder.stop() },
                onDiscard = { voiceRecorder.cancel() },
                onUse = {
                    // The take goes to the trim screen rather than straight onto the timeline, so
                    // its length is settled before it lands. Peaks are read before release()
                    // clears the recorder state.
                    val take = PendingAudioTake(
                        file = takeFile,
                        durationMicros = recorderState.elapsedMicros,
                        peaks = recorderState.waveform.peaks,
                    )
                    voiceRecorder.release()
                    onTake(take)
                },
            ),
            onClose = close,
        )
    }
}
