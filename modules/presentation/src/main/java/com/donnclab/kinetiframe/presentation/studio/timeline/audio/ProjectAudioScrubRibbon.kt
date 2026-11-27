package com.donnclab.kinetiframe.presentation.studio.timeline.audio

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
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.designsystem.theme.TimelineTokens
import com.donnclab.kinetiframe.media.audio.TimelineAudioSummary
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.studio.timeline.TimelineScrubPosition
import com.donnclab.kinetiframe.presentation.studio.timeline.frameCellWidth
import com.donnclab.kinetiframe.presentation.studio.timeline.timelineViewportOffsetFrames

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
    val blue = EditorTokens.playhead
    val cellPx = with(LocalDensity.current) { frameCellWidth.toPx() }
    val seek by rememberUpdatedState(onSeek)
    var dragFrame by remember { mutableIntStateOf(-1) }
    val description = stringResource(R.string.timeline_audio_scrubber)
    BoxWithConstraints(
        modifier.height(TimelineTokens.audioRibbonHeight).clipToBounds().semantics {
            contentDescription = description
        },
    ) {
        // Scroll position is read where it's used (drawing and gestures), never in composition,
        // so scrolling redraws the ribbon instead of recomposing it. Only the first visible
        // frame, which changes once per frame scrolled, drives composition.
        fun offsetFrames() = timelineViewportOffsetFrames(listState, cellPx)
        val first by remember(listState, cellPx) {
            derivedStateOf {
                // Center padding exposes earlier frames to the left of the logical scroll anchor.
                listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
                    ?: kotlin.math.floor(offsetFrames()).toInt().coerceAtLeast(0)
            }
        }
        val count = minOf((maxWidth / frameCellWidth).toInt() + 2, totalFrames - first).coerceAtLeast(0)
        val peaks = remember(clips, mutedTracks, first, count) {
            TimelineAudioSummary.window(clips, first, count, mutedTracks, samplesPerFrame = WAVE_SAMPLES_PER_FRAME)
        }
        fun select(x: Float, force: Boolean = false) {
            val padding = listState.layoutInfo.beforeContentPadding
            val frame = TimelineScrubPosition.frame(offsetFrames(), (x - padding) / cellPx, totalFrames)
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
            val scroll = offsetFrames()
            val edgePadding = listState.layoutInfo.beforeContentPadding
            val pixelOffset = (scroll - first) * cellPx
            val middle = size.height / 2
            peaks.forEachIndexed { i, peak ->
                val sampleWidth = cellPx / WAVE_SAMPLES_PER_FRAME
                val x = edgePadding + i * sampleWidth + sampleWidth / 2 - pixelOffset
                if (x >= -cellPx && x <= size.width + cellPx) {
                    val half = (peak * (middle - TimelineTokens.waveInset.toPx())).coerceAtLeast(
                        TimelineTokens.waveMinHalfHeight.toPx(),
                    )
                    drawLine(
                        blue.copy(alpha = .8f),
                        Offset(x, middle - half),
                        Offset(x, middle + half),
                        strokeWidth = TimelineTokens.waveStroke.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            val x = edgePadding + (currentFrame + .45f - scroll) * cellPx
            drawLine(blue, Offset(x, 0f), Offset(x, size.height), TimelineTokens.ribbonPlayheadStroke.toPx())
            drawCircle(
                blue,
                TimelineTokens.ribbonPlayheadDot.toPx(),
                Offset(
                    x,
                    size.height - TimelineTokens.ribbonPlayheadDot.toPx(),
                ),
            )
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
