// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.designsystem.theme

import androidx.compose.ui.unit.dp

/** Sizes for the editor's timeline: frame strip, transport, asset tracks and their markers. */
object TimelineTokens {
    // Frame strip.
    val frameCell = 40.dp
    val frameCellHeight = 48.dp
    val frameThumbWidth = 36.dp
    val frameThumbHeight = 40.dp
    val frameThumbCorner = 4.dp
    val currentFrameBorder = 2.dp
    val frameBadge = 12.dp
    val thumbDot = 2.dp
    val rulerHeight = 24.dp

    // Frame actions under the strip.
    val actionColumnWidth = 34.dp
    val actionButton = 30.dp
    val actionIcon = 17.dp
    val actionBadge = 14.dp
    val actionBadgeOffset = 6.dp

    // The context bar above a selected frame or clip.
    val contextBarElevation = 6.dp
    val contextActionPadding = 6.dp
    val contextLabelMaxWidth = 96.dp
    val contextBadge = 10.dp
    val contextMenuOffset = 56.dp

    // Transport and the row it sits in.
    val transportButtonWidth = 40.dp
    val transportButtonHeight = 48.dp
    val controlsHeight = 56.dp
    val controlsHeightWithAudio = 76.dp
    val controlsEndInset = 40.dp
    val compactTransportBreakpoint = 600.dp
    val speedSliderWidth = 260.dp
    val fpsTrackHeight = 2.dp
    val fpsThumb = 12.dp
    val fpsLabelWidth = 20.dp

    // Layers, onion skin and opacity.
    val layerThumb = 36.dp
    val checkerCell = 8.dp
    val onionPreviewHeight = 60.dp
    val onionPreviewInset = 24.dp
    val opacityChipWidth = 44.dp
    val opacityChipHeight = 28.dp
    val opacitySliderWidth = 220.dp

    /** A layer's eye and lock: a full-height touch target in the 54 dp row. */
    val layerToggle = 40.dp
    val chipCorner = 6.dp

    // Asset tracks.
    val trackGutter = 96.dp
    val trackHeight = 48.dp
    val trackHeaderHeight = 40.dp
    val trackLabelHeight = 24.dp
    val trackIconWidth = 24.dp
    val tracksPanelTopReserve = 120.dp
    val tracksPanelMin = 48.dp
    val tracksPanelMax = 168.dp
    val clipInset = 3.dp
    val clipCorner = 6.dp
    val clipBorderSelected = 2.dp
    val clipLabelInset = 6.dp
    val clipLabelInsetSelected = 20.dp
    val trimHandleWidth = 12.dp
    val playheadWidth = 2.dp
    val playheadOverhang = 24.dp

    // The centre pointer above the frame strip.
    val pointerWidth = 10.dp
    val pointerHeight = 7.dp

    // The project audio ribbon.
    val audioRibbonHeight = 20.dp
    val waveInset = 3.dp
    val waveMinHalfHeight = 0.6.dp
    val waveStroke = 1.5.dp
    val ribbonPlayheadStroke = 1.dp
    val ribbonPlayheadDot = 2.dp
}
