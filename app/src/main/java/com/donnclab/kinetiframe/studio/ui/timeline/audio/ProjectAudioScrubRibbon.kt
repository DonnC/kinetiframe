package com.donnclab.kinetiframe.studio.ui.timeline.audio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.audio.TimelineAudioSummary
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.project.model.AudioClipPlacement
import com.donnclab.kinetiframe.studio.ui.timeline.FRAME_CELL_WIDTH
import com.donnclab.kinetiframe.studio.ui.timeline.TimelineScrubPosition

/** Bounded waveform viewport sharing the filmstrip's coordinates, not a project-sized canvas. */
@Composable
fun ProjectAudioScrubRibbon(
    clips: Collection<AudioClipPlacement>,
    mutedTracks: Set<Int>,
    currentFrame: Int,
    totalFrames: Int,
    listState: LazyListState,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val blue = Color(0xFF5796F5)
    val cellPx = with(LocalDensity.current) { FRAME_CELL_WIDTH.toPx() }
    val seek by rememberUpdatedState(onSeek)
    var dragFrame by remember { mutableIntStateOf(-1) }
    BoxWithConstraints(
        modifier.height(20.dp).clipToBounds().semantics {
            contentDescription = "Project audio scrubber"
        },
    ) {
        val offsetFrames = com.donnclab.kinetiframe.studio.ui.timeline.timelineViewportOffsetFrames(
            listState,
            cellPx,
        )
        // Center padding exposes earlier frames to the left of the logical scroll anchor.
        val first = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
            ?: kotlin.math.floor(offsetFrames).toInt().coerceAtLeast(0)
        val pixelOffset = (offsetFrames - first) * cellPx
        val edgePadding = listState.layoutInfo.beforeContentPadding
        val latestPadding by rememberUpdatedState(edgePadding)
        val count = minOf((maxWidth / FRAME_CELL_WIDTH).toInt() + 2, totalFrames - first).coerceAtLeast(0)
        val peaks = remember(clips, mutedTracks, first, count) {
            TimelineAudioSummary.window(clips, first, count, mutedTracks, samplesPerFrame = WAVE_SAMPLES_PER_FRAME)
        }
        val latestOffset by rememberUpdatedState(offsetFrames)
        fun select(x: Float, force: Boolean = false) {
            val frame = TimelineScrubPosition.frame(latestOffset, (x - latestPadding) / cellPx, totalFrames)
            if (force || frame != dragFrame) {
                dragFrame = frame
                seek(frame)
            }
        }
        Canvas(
            Modifier.fillMaxSize()
                .pointerInput(cellPx, totalFrames) { detectTapGestures { select(it.x, force = true) } }
                .pointerInput(cellPx, totalFrames) {
                    detectDragGestures(onDragStart = { select(it.x, force = true) }) { change, _ ->
                        change.consume()
                        select(change.position.x)
                    }
                },
        ) {
            val middle = size.height / 2
            peaks.forEachIndexed { i, peak ->
                val sampleWidth = cellPx / WAVE_SAMPLES_PER_FRAME
                val x = edgePadding + i * sampleWidth + sampleWidth / 2 - pixelOffset
                if (x >= -cellPx && x <= size.width + cellPx) {
                    val half = (peak * (middle - 3.dp.toPx())).coerceAtLeast(.6.dp.toPx())
                    drawLine(
                        blue.copy(alpha = .8f),
                        Offset(x, middle - half),
                        Offset(x, middle + half),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            val x = edgePadding + (currentFrame + .45f - offsetFrames) * cellPx
            drawLine(blue, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
            drawCircle(blue, 2.dp.toPx(), Offset(x, size.height - 2.dp.toPx()))
        }
    }
}

private const val WAVE_SAMPLES_PER_FRAME = 8

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun ProjectAudioScrubRibbonPreview() {
    KinetiFrameTheme {
        ProjectAudioScrubRibbon(
            listOf(
                AudioClipPlacement(
                    "demo",
                    "audio",
                    0,
                    "Voice",
                    0,
                    24,
                    waveformPeaks = List(24) { if (it in 4..18) .6f else 0f },
                ),
            ),
            emptySet(),
            4,
            24,
            rememberLazyListState(),
            {},
        )
    }
}
