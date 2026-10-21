package com.donnclab.kinetiframe.ui.components.timeline

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Virtual coordinates keep long takes within Compose's finite layout constraints. */
@Composable
internal fun TimelineTrackViewport(
    frames: Int,
    filmstrip: LazyListState,
    modifier: Modifier = Modifier,
    currentFrame: Int = 0,
    onScrubFrame: (Int) -> Unit = {},
    content: @Composable (offsetFrames: Float, visibleFrames: Float) -> Unit,
) {
    BoxWithConstraints(modifier.clipToBounds()) {
        val cellPx = with(LocalDensity.current) { FRAME_CELL_WIDTH.toPx() }
        val visibleFrames = maxWidth / FRAME_CELL_WIDTH
        var offsetPx by remember { mutableFloatStateOf(0f) }
        val selected by rememberUpdatedState(currentFrame)
        val scrub by rememberUpdatedState(onScrubFrame)
        val maximum by rememberUpdatedState((frames - 1).coerceAtLeast(0) * cellPx)
        val scroll = rememberScrollableState { delta ->
            val previous = offsetPx
            offsetPx = (offsetPx - delta).coerceIn(0f, maximum)
            previous - offsetPx
        }
        LaunchedEffect(filmstrip, cellPx) {
            snapshotFlow { timelineViewportOffsetFrames(filmstrip, cellPx) }
                .collect { framesOffset ->
                    if (!scroll.isScrollInProgress) offsetPx = framesOffset * cellPx
                }
        }
        LaunchedEffect(scroll, filmstrip, cellPx) {
            var anchor = .5f
            var wasScrolling = false
            var lastFrame = -1
            snapshotFlow { offsetPx to scroll.isScrollInProgress }.collect { (px, scrolling) ->
                if (scrolling) {
                    if (!wasScrolling) {
                        anchor = (selected + .5f - px / cellPx).coerceIn(.5f, visibleFrames.coerceAtLeast(.5f))
                        lastFrame = selected
                    }
                    val frame = TimelineScrubPosition.frame(px / cellPx, anchor, frames)
                    if (frame != lastFrame) {
                        lastFrame = frame
                        scrub(frame)
                    }
                    val (index, offset) = TimelineScrollSync.pixelsToItem(px.toInt(), cellPx)
                    filmstrip.scrollToItem(
                        index.coerceAtMost((filmstrip.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)),
                        offset,
                    )
                }
                wasScrolling = scrolling
            }
        }
        Box(Modifier.fillMaxWidth().scrollable(scroll, Orientation.Horizontal)) {
            content(offsetPx / cellPx, visibleFrames)
        }
    }
}
