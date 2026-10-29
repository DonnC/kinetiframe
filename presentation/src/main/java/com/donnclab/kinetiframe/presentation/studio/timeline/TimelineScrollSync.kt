package com.donnclab.kinetiframe.presentation.studio.timeline

import kotlin.math.roundToInt

/**
 * Pure pixel<->item conversion for keeping the actor/audio/background track lanes (each a plain
 * pixel-scrolled [androidx.compose.foundation.horizontalScroll] Box, since a clip can span many
 * frames and isn't one-widget-per-frame) in lockstep with the frame filmstrip ([androidx.compose.
 * foundation.lazy.LazyRow], since it can hold hundreds of frame thumbnails and needs LazyRow's
 * windowed composition to stay cheap). Both scroll spaces represent the exact same horizontal
 * ruler -- [com.donnclab.kinetiframe.presentation.studio.timeline.FRAME_CELL_WIDTH]... see that constant's doc
 * comment -- per frame, so converting between "raw scrolled pixels" and "first visible item index
 * plus that item's own scroll offset" is exact, not an approximation, as long as every item really
 * is [cellWidthPx] wide end to end (true for [FrameCard]'s fixed 36dp card + 4dp `spacedBy` gap,
 * which together equal `FRAME_CELL_WIDTH`, and true for a track lane rendering one cell per frame).
 *
 * Kept as pure functions with no Compose types so this conversion has a plain JVM unit test
 * ([TimelineScrollSyncTest]) independent of a Compose UI test harness -- this project has no
 * Gradle/compile toolchain available to run one, so this is the one piece that can actually be
 * verified mechanically.
 */
object TimelineScrollSync {
    /** Where a LazyListState of whole [cellWidthPx]-wide items would need to report itself
     *  (first visible item index, and that item's own scroll offset) to represent the same ruler
     *  position as a plain [androidx.compose.foundation.gestures.ScrollState] sitting at
     *  [pixels]. Negative or non-finite input is treated as the very start of the ruler. */
    fun pixelsToItem(pixels: Int, cellWidthPx: Float): Pair<Int, Int> {
        if (cellWidthPx <= 0f || pixels <= 0) return 0 to 0
        val index = (pixels / cellWidthPx).toInt()
        val offset = (pixels - index * cellWidthPx).roundToInt().coerceIn(0, cellWidthPx.roundToInt())
        return index to offset
    }

    /** The raw scroll-pixel position that corresponds to a LazyListState sitting at
     *  [firstVisibleItemIndex] with [firstVisibleItemScrollOffset] pixels into that item. */
    fun itemToPixels(firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int, cellWidthPx: Float): Int =
        (firstVisibleItemIndex * cellWidthPx).roundToInt() + firstVisibleItemScrollOffset
}
