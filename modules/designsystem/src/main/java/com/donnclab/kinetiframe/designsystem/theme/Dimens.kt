package com.donnclab.kinetiframe.designsystem.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Spacing, sizing, radius and type-size scale shared by the app's screens. Using these steps
 * instead of inline `.dp` literals keeps cards, chips and rows visually consistent.
 */
object Dimens {
    // Spacing scale -- use the closest step rather than inventing a one-off value.
    val spaceXxxs = 1.dp
    val spaceXxs = 2.dp
    val spaceXxsPlus = 3.dp
    val spaceXs = 4.dp
    val spaceXsPlus = 6.dp
    val spaceS = 8.dp
    val spaceSPlus = 10.dp
    val spaceM = 12.dp
    val spaceMPlus = 14.dp
    val spaceL = 16.dp
    val spaceXl = 20.dp
    val spaceXlPlus = 24.dp
    val spaceXxl = 28.dp
    val spaceXxxl = 32.dp

    // Corner radii.
    val radiusXxs = 4.dp
    val radiusXs = 6.dp
    val radiusS = 8.dp
    val radiusSPlus = 10.dp
    val radiusM = 12.dp
    val radiusMPlus = 14.dp
    val radiusL = 16.dp
    val radiusPill = 20.dp

    // Icon sizes.
    val iconXs = 11.dp
    val iconS = 16.dp
    val iconSPlus = 18.dp
    val iconM = 20.dp
    val iconMPlus = 22.dp
    val iconL = 24.dp

    // Type sizes for the compact card/chip text this feature uses (body copy still comes from
    // MaterialTheme.typography; these are for the smaller, denser labels a card overlay needs).
    val textXs = 9.5.sp
    val textS = 10.5.sp
    val textM = 12.sp
    val textL = 13.5.sp
    val textXl = 18.sp
    val textDisplay = 20.sp

    // Component sizing.

    /** Material's minimum touch target. */
    val touchTarget = 48.dp
    val topBarIconButton = 38.dp
    val iconButtonS = 28.dp
    val iconButton = 40.dp
    val avatarSize = 24.dp
    val chipHeight = 22.dp
    val gridCardThumbHeight = 150.dp
    val gridCardThumbHeightCompact = 132.dp
    val playButtonSize = 42.dp
    val bottomSheetHandleWidth = 40.dp
    val bottomSheetHandleHeight = 4.dp
    val stampThumbSize = 72.dp
    val waveformBarWidth = 2.5.dp
    val waveformHeight = 16.dp
    val actionButtonHeight = 52.dp
    val bottomNavHeight = 78.dp
    val compactFieldHeight = 36.dp
    val menuTileWidth = 64.dp
    val sliderPopupWidth = 192.dp
    val sliderHeight = 24.dp
    val sliderThumb = 16.dp
    val sliderTrackHeight = 2.dp

    // The brand bar's badge and wordmark.
    val brandBadge = 32.dp
    val brandWordmarkWidth = 140.dp
    val brandWordmarkHeight = 20.dp

    /** The bouncing-ball frame-rate preview and New Project's canvas shape cards. */
    val fpsPreviewHeight = 140.dp
    val canvasShapeCard = 76.dp
    val canvasShapeMax = 44.dp

    // Surface elevation.
    val elevationFlat = 2.dp
    val elevationS = 3.dp
    val elevationRaised = 4.dp
    val elevationFloating = 6.dp
    val elevationM = 8.dp
    val elevationL = 12.dp

    // Border widths.
    val borderThin = 1.dp
    val borderMedium = 1.5.dp
    val borderThick = 2.dp

    // Audio recorder.
    val recorderWaveformHeight = 132.dp
    val recorderBarWidth = 3.dp
    val recorderBarGap = 2.dp
    val recorderButtonSize = 72.dp
    val recorderButtonInner = 28.dp

    // Wide enough for the discard, record and "Add to timeline" controls on one line.
    val recorderSheetMaxWidth = 480.dp

    // Grid layout.
    val gridColumnGap = 14.dp
    val gridColumns = 2
}
