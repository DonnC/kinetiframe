package com.donnclab.kinetiframe.presentation.media.audio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.media.audio.Waveform

/**
 * A placed clip's waveform, resampled to whatever width the clip currently occupies.
 *
 * Resampling per draw (rather than storing peaks at a fixed bar count) keeps the outline
 * accurate while a clip is trimmed or the timeline is zoomed: the same audio always reads as the
 * same shape, just at a different density. [Waveform.downsampled] averages within each bucket, so
 * narrowing a clip dims transients rather than dropping them out entirely.
 *
 * Drawn as a filled mirror about the centre line rather than as separate bars, because a placed
 * clip is only ~28dp tall -- at that size discrete bars alias into noise, whereas a solid outline
 * still reads as loud-versus-quiet at a glance.
 */
@Composable
fun AudioClipWaveform(waveform: Waveform, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize().drawWithCache {
            val barWidth = BAR_WIDTH_DP.dp.toPx()
            val step = barWidth + BAR_GAP_DP.dp.toPx()
            val bars = (size.width / step).toInt().coerceIn(1, 2048)
            val peaks = waveform.downsampled(bars).peaks
            val centreY = size.height / 2f
            val minHalf = barWidth / 2f
            val spacing = size.width / peaks.size.coerceAtLeast(1)
            onDrawBehind {
                peaks.forEachIndexed { index, peak ->
                    val x = (index + .5f) * spacing
                    if (x > size.width) return@forEachIndexed
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
        },
    )
}

private const val BAR_WIDTH_DP = 1.5f
private const val BAR_GAP_DP = 1f

@Preview(name = "Clip waveform -- light", showBackground = true, widthDp = 200, heightDp = 32)
@Preview(
    name = "Clip waveform -- dark",
    showBackground = true,
    widthDp = 200,
    heightDp = 32,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AudioClipWaveformPreview() {
    KinetiFrameTheme {
        AudioClipWaveform(
            waveform = Waveform(
                List(120) { index ->
                    (0.15f + 0.75f * kotlin.math.abs(kotlin.math.sin(index / 7f))).coerceIn(0f, 1f)
                },
            ),
            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        )
    }
}
