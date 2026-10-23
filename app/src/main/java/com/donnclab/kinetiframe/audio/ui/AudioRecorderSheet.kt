package com.donnclab.kinetiframe.audio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Square
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.X
import com.donnclab.kinetiframe.R
import com.donnclab.kinetiframe.audio.VoiceRecorder
import com.donnclab.kinetiframe.audio.Waveform
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

/**
 * A finished take, handed from the recorder to the trim screen.
 *
 * Carries its own peaks because the live meter already measured them -- the trim screen draws the
 * take the animator just watched rather than decoding the file again to redraw the same picture.
 */
data class PendingAudioTake(val file: java.io.File, val durationMicros: Long, val peaks: List<Float>)

/** The four things the recorder can be asked to do, grouped to keep the screen at 4 parameters. */
data class AudioRecorderActions(
    val onRecord: () -> Unit,
    val onStop: () -> Unit,
    val onDiscard: () -> Unit,
    val onUse: () -> Unit,
)

/**
 * Voice-over capture: a live level meter, a running timer, and one primary control that is a
 * record dot while idle and a stop square while capturing.
 *
 * Replaces an `AlertDialog` whose entire body was the word "Recording..." -- with no meter there
 * was nothing distinguishing a working microphone from a silently failed one, and no way to judge
 * a take before committing it. Rendering is a pure function of [VoiceRecorder.State], so the
 * screen previews (and can be UI-tested) without a microphone.
 *
 * The record control is a drawn circle rather than an icon deliberately: a filled dot is the
 * universal record symbol, and this app's icon set has no microphone glyph.
 */
@Composable
fun AudioRecorderSheet(
    state: VoiceRecorder.State,
    actions: AudioRecorderActions,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusL),
        tonalElevation = Dimens.elevationS,
    ) {
        Column(Modifier.padding(Dimens.spaceL), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.audio_recorder_title), style = MaterialTheme.typography.titleMedium)
                Box(Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(Lucide.X, contentDescription = stringResource(R.string.action_close))
                }
            }

            LiveWaveform(
                waveform = state.waveform,
                color = if (state.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = Dimens.spaceM),
            )

            Text(
                text = formatElapsed(state.elapsedMicros),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = state.error
                    ?: when {
                        state.isRecording -> stringResource(R.string.audio_recording)
                        state.hasTake -> stringResource(R.string.audio_take_ready)
                        else -> stringResource(R.string.audio_start_hint)
                    },
                style = MaterialTheme.typography.bodySmall,
                color = if (state.error != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(top = Dimens.spaceXs),
            )

            Row(
                Modifier.fillMaxWidth().padding(top = Dimens.spaceL),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.hasTake && !state.isRecording) {
                    IconButton(onClick = actions.onDiscard) {
                        Icon(Lucide.Trash2, contentDescription = stringResource(R.string.audio_discard_take))
                    }
                }
                RecordControl(
                    isRecording = state.isRecording,
                    onClick = if (state.isRecording) actions.onStop else actions.onRecord,
                )
            }

            if (state.hasTake && !state.isRecording) {
                TextButton(
                    onClick = actions.onUse,
                    modifier = Modifier.padding(top = Dimens.spaceS),
                ) { Text(stringResource(R.string.audio_add_to_timeline)) }
            }
        }
    }
}

/** Record dot while idle, stop square while capturing -- one control, so the primary action never
 *  moves under the animator's thumb mid-take. */
@Composable
private fun RecordControl(isRecording: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(Dimens.recorderButtonSize),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isRecording) {
                Icon(
                    Lucide.Square,
                    contentDescription = stringResource(R.string.audio_stop_recording),
                    tint = MaterialTheme.colorScheme.error,
                )
            } else {
                Box(
                    Modifier
                        .size(Dimens.recorderButtonInner)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                )
            }
        }
    }
}

/** mm:ss.t -- tenths matter when trimming a take to a beat. */
private fun formatElapsed(micros: Long): String {
    val totalTenths = micros / 100_000L
    val minutes = totalTenths / 600
    val seconds = (totalTenths / 10) % 60
    val tenths = totalTenths % 10
    return "%02d:%02d.%d".format(minutes, seconds, tenths)
}

@Preview(name = "Recorder -- light", showBackground = true)
@Preview(name = "Recorder -- dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AudioRecorderSheetPreview() {
    KinetiFrameTheme {
        AudioRecorderSheet(
            state = VoiceRecorder.State(
                isRecording = true,
                elapsedMicros = 4_617_000L,
                waveform = Waveform(
                    List(70) { index ->
                        (0.2f + 0.7f * kotlin.math.abs(kotlin.math.sin(index / 5f))).coerceIn(0f, 1f)
                    },
                ),
            ),
            actions = AudioRecorderActions({}, {}, {}, {}),
            onClose = {},
        )
    }
}
