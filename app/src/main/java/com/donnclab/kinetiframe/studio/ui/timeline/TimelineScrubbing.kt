package com.donnclab.kinetiframe.studio.ui.timeline

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.floor

/** Shared frame mapping for the filmstrip, performance strip and audio ruler. */
internal object TimelineScrubPosition {
    fun nearestCenter(centers: List<Pair<Int, Float>>, viewportCenter: Float): Int =
        centers.minByOrNull { kotlin.math.abs(it.second - viewportCenter) }?.first ?: 0
    fun frame(offsetFrames: Float, anchorFrames: Float, count: Int): Int =
        floor(offsetFrames + anchorFrames).toInt().coerceIn(0, (count - 1).coerceAtLeast(0))
}

internal fun centeredTimelineFrame(list: LazyListState): Int {
    val layout = list.layoutInfo
    return TimelineScrubPosition.nearestCenter(
        layout.visibleItemsInfo.map { it.index to (it.offset + it.size / 2f) },
        (layout.viewportStartOffset + layout.viewportEndOffset) / 2f,
    )
}

/** Uses measured item coordinates so centered edge padding cannot desynchronise audio/track lanes. */
internal fun timelineViewportOffsetFrames(list: LazyListState, cellPx: Float): Float {
    val layout = list.layoutInfo
    val item = layout.visibleItemsInfo.firstOrNull() ?: return 0f
    // Lazy item offsets exclude the visual content padding applied during placement.
    return item.index - item.offset / cellPx
}

/** Only real drags (including their fling) audition frames. Playback auto-scroll never does. */
@Composable
internal fun ObserveTimelineScrubbing(
    list: LazyListState,
    cellWidth: Dp,
    currentFrame: Int,
    frameCount: Int,
    onFrame: (Int) -> Unit,
    onStart: () -> Unit = {},
) {
    val cellPx = with(LocalDensity.current) { cellWidth.toPx() }
    val selected by rememberUpdatedState(currentFrame)
    val count by rememberUpdatedState(frameCount)
    val select by rememberUpdatedState(onFrame)
    val start by rememberUpdatedState(onStart)
    LaunchedEffect(list, cellPx) {
        coroutineScope {
            var dragging by mutableStateOf(false)
            var auditioning by mutableStateOf(false)
            var lastFrame = -1
            launch {
                list.interactionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is DragInteraction.Start -> {
                            lastFrame = selected
                            dragging = true
                            auditioning = true
                            start()
                        }

                        is DragInteraction.Stop, is DragInteraction.Cancel -> dragging = false
                    }
                }
            }
            snapshotFlow {
                Triple(
                    centeredTimelineFrame(list),
                    list.isScrollInProgress,
                    dragging,
                )
            }.collect { (centerFrame, scrolling, fingerDown) ->
                if (auditioning && scrolling && count > 0) {
                    val frame = centerFrame.coerceIn(0, count - 1)
                    if (frame != lastFrame) {
                        lastFrame = frame
                        select(frame)
                    }
                }
                if (!scrolling && !fingerDown) auditioning = false
            }
        }
    }
}
