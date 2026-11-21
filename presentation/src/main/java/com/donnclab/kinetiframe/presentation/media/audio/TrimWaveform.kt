package com.donnclab.kinetiframe.presentation.media.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.media.audio.AudioTrimSelection
import com.donnclab.kinetiframe.media.audio.Waveform

/**
 * The take's whole waveform with the kept region highlighted between two draggable handles, and a
 * playhead tracking preview position.
 *
 * The excluded head and tail stay visible, dimmed, rather than being cropped away: an animator
 * trimming by ear needs to see what they are cutting in order to move a handle back, and a view
 * that only showed the selection would make widening it a blind gesture.
 *
 * Handle drags are reported as 0f..1f fractions rather than pixels so the maths that clamps them
 * ([AudioTrimSelection]) stays independent of how wide this happens to be drawn.
 */
@Composable
fun TrimWaveform(
    state: AudioTrimState,
    onSelectionChange: (AudioTrimSelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val waveform = state.waveform
    val selection = state.selection
    val playheadMicros = state.playheadMicros
    val latestSelection by rememberUpdatedState(selection)
    val changeSelection by rememberUpdatedState(onSelectionChange)
    var gestureSelection by remember { mutableStateOf(selection) }
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(Dimens.radiusM))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        val trackWidth: Dp = maxWidth
        val widthPx = with(LocalDensity.current) { trackWidth.toPx() }

        AudioClipWaveform(
            waveform = waveform,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxSize(),
        )
        // Kept region, drawn over the full outline so the excluded parts read as dimmed rather
        // than as a different waveform.
        Box(
            Modifier
                .offset(x = trackWidth * selection.inFraction)
                .width(trackWidth * (selection.outFraction - selection.inFraction).coerceAtLeast(0f))
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = SELECTION_ALPHA)),
        )
        if (playheadMicros > 0L) {
            val playheadFraction = (playheadMicros.toFloat() / selection.sourceDurationMicros).coerceIn(0f, 1f)
            Box(
                Modifier
                    .offset(x = trackWidth * playheadFraction)
                    .width(PLAYHEAD_WIDTH.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.error),
            )
        }
        TrimEdge(
            offsetX = trackWidth * selection.inFraction,
            onDragStart = { gestureSelection = latestSelection },
            onDragCancel = { changeSelection(gestureSelection) },
            onDragFraction = { delta ->
                changeSelection(gestureSelection.withInFraction(gestureSelection.inFraction + delta))
            },
            widthPx = widthPx,
        )
        TrimEdge(
            offsetX = trackWidth * selection.outFraction - HANDLE_WIDTH.dp,
            onDragStart = { gestureSelection = latestSelection },
            onDragCancel = { changeSelection(gestureSelection) },
            onDragFraction = { delta ->
                changeSelection(gestureSelection.withOutFraction(gestureSelection.outFraction + delta))
            },
            widthPx = widthPx,
        )
    }
}

/** One draggable edge. Kept deliberately wide relative to its visual line so the touch target
 *  clears the minimum comfortable size without the handle itself looking heavy. */
@Composable
private fun TrimEdge(
    offsetX: Dp,
    onDragFraction: (Float) -> Unit,
    widthPx: Float,
    onDragStart: () -> Unit,
    onDragCancel: () -> Unit,
) {
    val drag by rememberUpdatedState(onDragFraction)
    val start by rememberUpdatedState(onDragStart)
    val cancel by rememberUpdatedState(onDragCancel)
    Box(
        Modifier
            .offset(x = offsetX)
            .width(HANDLE_WIDTH.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(Dimens.radiusM))
            .pointerInput(widthPx) {
                var distance = 0f
                detectHorizontalDragGestures(onDragStart = {
                    distance = 0f
                    start()
                }, onDragCancel = { cancel() }) { change, dragAmount ->
                    change.consume()
                    distance += dragAmount
                    if (widthPx > 0f) drag(distance / widthPx)
                }
            },
    )
}

private const val SELECTION_ALPHA = 0.22f
private const val HANDLE_WIDTH = 24
private const val PLAYHEAD_WIDTH = 2

@Preview(name = "Trim waveform -- light", showBackground = true, widthDp = 340, heightDp = 140)
@Preview(
    name = "Trim waveform -- dark",
    showBackground = true,
    widthDp = 340,
    heightDp = 140,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun TrimWaveformPreview() {
    KinetiFrameTheme {
        TrimWaveform(
            state = AudioTrimState(
                waveform = Waveform(
                    List(140) { index ->
                        (0.15f + 0.75f * kotlin.math.abs(kotlin.math.sin(index / 9f))).coerceIn(0f, 1f)
                    },
                ),
                selection = AudioTrimSelection(
                    sourceDurationMicros = 5_000_000L,
                    inMicros = 1_565_000L,
                    outMicros = 3_565_000L,
                ),
                playheadMicros = 2_968_000L,
            ),
            onSelectionChange = {},
        )
    }
}
