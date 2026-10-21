package com.donnclab.kinetiframe.ui.components.audio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.engine.audio.Waveform

/**
 * Draws a [Waveform] as centre-anchored vertical bars, newest on the right.
 *
 * Rendered with Compose's own Canvas rather than a charting or audio-visualiser dependency: the
 * shape is one line per peak mirrored about the middle, so a library would add weight without
 * adding capability. Bars are drawn from the tail of the peak list so a take longer than the
 * visible width scrolls instead of squashing, which keeps the meter's motion readable as feedback
 * that the microphone is actually live.
 */
@Composable
fun LiveWaveform(waveform: Waveform, color: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .fillMaxWidth()
            .height(Dimens.recorderWaveformHeight),
    ) {
        val barWidth = Dimens.recorderBarWidth.toPx()
        val gap = Dimens.recorderBarGap.toPx()
        val step = barWidth + gap
        if (step <= 0f) return@Canvas
        val capacity = (size.width / step).toInt().coerceAtLeast(1)
        val visible = waveform.peaks.takeLast(capacity)
        val centreY = size.height / 2f
        // A floor so silence still reads as a thin live line rather than a blank panel -- an empty
        // canvas is indistinguishable from a broken microphone.
        val minHalf = barWidth / 2f
        visible.forEachIndexed { index, peak ->
            val x = index * step + barWidth / 2f
            val halfHeight = (peak * centreY).coerceAtLeast(minHalf)
            drawLine(
                color = color,
                start = Offset(x, centreY - halfHeight),
                end = Offset(x, centreY + halfHeight),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Preview(name = "Waveform -- light", showBackground = true, widthDp = 320)
@Preview(
    name = "Waveform -- dark",
    showBackground = true,
    widthDp = 320,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LiveWaveformPreview() {
    KinetiFrameTheme {
        val sample = Waveform(
            List(80) { index ->
                val phase = index / 6f
                (0.25f + 0.6f * kotlin.math.abs(kotlin.math.sin(phase))).coerceIn(0f, 1f)
            },
        )
        LiveWaveform(
            waveform = sample,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(Dimens.spaceL),
        )
    }
}
