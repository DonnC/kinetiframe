// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.timeline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.TimelineTokens

/**
 * How big the timeline draws a frame. The frame strip, the track lanes, their ruler and the audio
 * ribbon all read the same scale, so a clip still lines up with the frames it covers whatever the
 * size. A phone keeps the small strip; the tablet workspace has room for frames you can read.
 */
@Immutable
data class TimelineScale(
    val thumbWidth: Dp,
    val thumbHeight: Dp,
    /** The strip's height: thumbnails plus room for the current frame's border. */
    val stripHeight: Dp,
    /** Full-size frame numbers; the phone's small cards need the compact ones. */
    val largeLabels: Boolean,
) {
    /** One frame's width on every lane: its thumbnail and the gap after it. */
    val cell: Dp get() = thumbWidth + TimelineTokens.frameGap

    companion object {
        val Phone = TimelineScale(
            thumbWidth = TimelineTokens.frameThumbWidth,
            thumbHeight = TimelineTokens.frameThumbHeight,
            stripHeight = Dimens.touchTarget,
            largeLabels = false,
        )

        /** The tablet workspace's strip: frames shaped like the canvas, at a height you can read. */
        fun wide(stage: StageDimensions): TimelineScale {
            val height = TimelineTokens.wideThumbHeight
            val width = (height * (stage.width.toFloat() / stage.height))
                .coerceIn(TimelineTokens.wideThumbMinWidth, TimelineTokens.wideThumbMaxWidth)
            return TimelineScale(width, height, height + TimelineTokens.wideStripPadding, largeLabels = true)
        }
    }
}

val LocalTimelineScale = staticCompositionLocalOf { TimelineScale.Phone }

/** One frame's width on the timeline, the same on the strip and every lane. */
internal val frameCellWidth: Dp
    @Composable @ReadOnlyComposable
    get() = LocalTimelineScale.current.cell
