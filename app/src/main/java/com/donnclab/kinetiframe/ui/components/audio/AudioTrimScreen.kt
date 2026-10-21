package com.donnclab.kinetiframe.ui.components.audio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.X
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.engine.audio.AudioTrimSelection
import com.donnclab.kinetiframe.engine.audio.Waveform
import com.donnclab.kinetiframe.ui.common.strings.FeatureStrings

/** Everything the trim screen renders, grouped to keep the screen inside the 4-parameter budget. */
data class AudioTrimState(
    val waveform: Waveform,
    val selection: AudioTrimSelection,
    val playheadMicros: Long = 0L,
    val isPreviewing: Boolean = false,
)

/** What the trim screen can ask for. */
data class AudioTrimActions(
    val onSelectionChange: (AudioTrimSelection) -> Unit,
    val onTogglePreview: () -> Unit,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit,
)

/**
 * The step between capturing a take and placing it: choose which part of the recording actually
 * becomes a clip.
 *
 * This exists because trimming after placement is the wrong moment -- once a clip is on the
 * timeline its length is entangled with where it sits, so tightening the top of a take also means
 * re-dragging it back into sync. Deciding in/out first means the clip lands at the length it was
 * meant to be.
 *
 * The readouts show in, out and resulting length together: length is the number that decides how
 * many frames the clip will occupy, and computing it in your head from two timestamps is exactly
 * the sort of friction that makes people give up on trimming.
 */
@Composable
fun AudioTrimScreen(state: AudioTrimState, actions: AudioTrimActions, modifier: Modifier = Modifier) {
    var preciseTrim by remember { mutableStateOf(false) }
    if (preciseTrim) {
        AudioTrimTimeDialog(state.selection, { preciseTrim = false }, {
            actions.onSelectionChange(it)
            preciseTrim = false
        })
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusL),
        tonalElevation = Dimens.elevationS,
    ) {
        Column(Modifier.padding(Dimens.spaceL)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(FeatureStrings.Audio.TRIM_TITLE, style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = actions.onCancel) {
                        Icon(Lucide.X, contentDescription = FeatureStrings.Audio.CLOSE)
                    }
                }
            }

            TrimWaveform(
                state = state,
                onSelectionChange = actions.onSelectionChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.recorderWaveformHeight)
                    .padding(vertical = Dimens.spaceM),
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrimReadout(FeatureStrings.Audio.TRIM_IN, state.selection.inMicros) { preciseTrim = true }
                IconButton(onClick = actions.onTogglePreview) {
                    Icon(
                        imageVector = if (state.isPreviewing) Lucide.Pause else Lucide.Play,
                        contentDescription = if (state.isPreviewing) {
                            FeatureStrings.Audio.STOP_PREVIEW
                        } else {
                            FeatureStrings.Audio.PREVIEW
                        },
                    )
                }
                TrimReadout(FeatureStrings.Audio.TRIM_OUT, state.selection.outMicros) { preciseTrim = true }
            }

            Text(
                "${FeatureStrings.Audio.TRIM_LENGTH}  ${formatTimestamp(state.selection.selectedDurationMicros)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.spaceXs),
            )

            Button(
                onClick = actions.onConfirm,
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.spaceL),
            ) { Text(FeatureStrings.Audio.ADD_TO_TIMELINE) }
        }
    }
}

@Composable
private fun TrimReadout(label: String, micros: Long, onClick: () -> Unit) {
    Column(
        Modifier.clickable(onClick = onClick).padding(Dimens.spaceS),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatTimestamp(micros), style = MaterialTheme.typography.labelLarge)
    }
}

/** mm:ss.mmm -- milliseconds are shown because a trim is routinely judged against a beat that
 *  tenths cannot express. */
private fun formatTimestamp(micros: Long): String {
    val totalMillis = micros / 1_000L
    val minutes = totalMillis / 60_000L
    val seconds = (totalMillis / 1_000L) % 60L
    val millis = totalMillis % 1_000L
    return "%02d:%02d.%03d".format(minutes, seconds, millis)
}

@Preview(name = "Trim -- light", showBackground = true)
@Preview(name = "Trim -- dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AudioTrimScreenPreview() {
    KinetiFrameTheme {
        AudioTrimScreen(
            state = AudioTrimState(
                waveform = Waveform(
                    List(140) { index ->
                        (0.15f + 0.75f * kotlin.math.abs(kotlin.math.sin(index / 9f))).coerceIn(0f, 1f)
                    },
                ),
                selection = AudioTrimSelection(5_000_000L, 1_565_000L, 3_565_000L),
                playheadMicros = 2_968_000L,
                isPreviewing = true,
            ),
            actions = AudioTrimActions({}, {}, {}, {}),
        )
    }
}
