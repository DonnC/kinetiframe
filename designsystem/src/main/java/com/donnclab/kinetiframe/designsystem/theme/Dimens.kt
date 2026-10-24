package com.donnclab.kinetiframe.designsystem.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Spacing, sizing, radius and type-size scale shared by the app's screens. Using these steps
 * instead of inline `.dp` literals keeps cards, chips and rows visually consistent.
 */
object Dimens {
    // Spacing scale -- use the closest step rather than inventing a one-off value.
    val spaceXs = 4.dp
    val spaceS = 8.dp
    val spaceM = 12.dp
    val spaceL = 16.dp
    val spaceXl = 20.dp
    val spaceXxl = 28.dp

    // Corner radii.
    val radiusS = 8.dp
    val radiusM = 12.dp
    val radiusL = 16.dp
    val radiusPill = 20.dp

    // Icon sizes.
    val iconXs = 11.dp
    val iconS = 16.dp
    val iconM = 20.dp
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
    val topBarIconButton = 38.dp
    val avatarSize = 24.dp
    val chipHeight = 22.dp
    val gridCardThumbHeight = 150.dp
    val gridCardThumbHeightCompact = 132.dp
    val packCollageThumbHeight = 110.dp
    val playButtonSize = 42.dp
    val bottomSheetHandleWidth = 40.dp
    val bottomSheetHandleHeight = 4.dp
    val stampThumbSize = 72.dp
    val waveformBarWidth = 2.5.dp
    val waveformHeight = 16.dp
    val actionButtonHeight = 52.dp

    // Surface elevation.
    val elevationS = 3.dp

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
