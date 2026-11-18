package com.donnclab.kinetiframe.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object ActorStudioTokens {
    val panelSurface = BrandNightSurface
    val panelSurfaceVariant = BrandNightVariant
    val panelDivider = Color(0xFF414F46)
    val onPanel = BrandIvory
    val onPanelMuted = Color(0xFFBFCBBF)

    // This panel is always dark, even in the light app theme.
    val accent = BrandMint

    /** Content on [accent], such as an enabled apply icon. */
    val onAccent = Color(0xFF0B1416)

    /** The sketch editor's binding region and its outline. */
    val sketchBind = Color(0xFF32B6AC)
    val partRailIdle = Color(0xFFF1EBF3)
    val presetIconStroke = Color(0xFF172B36)
    val panelWidth = 320.dp

    // Floating bars over the stage (part rail, part actions).
    val floatingElevation = 6.dp
    val partChipCorner = 18.dp
    val partDot = 6.dp

    // Mode pill and top bar.
    val pillPadding = 3.dp
    val pillCorner = 17.dp
    val pillSelectedElevation = 2.dp
    val pillIconPadding = 11.dp
    val topBarCompactBreakpoint = 480.dp
    val hintStartInset = 144.dp
    val labelPaddingVertical = 5.dp

    // Rig overlay drawn on the stage.
    val rigStroke = 2.dp
    val rootStroke = 3.dp
    val pivotRadius = 12.dp
    val pivotRing = 3.dp
    val jointRadius = 8.dp
    val jointEndRadius = 6.dp
    val jointSelectedRadius = 9.dp

    // Sketch editor.
    val sketchHitRadius = 24.dp
    val sketchHandleRadius = 6.dp
    val sketchTieTolerance = 3.dp
    val sketchSwatch = 32.dp
    val sketchSliderWidth = 160.dp

    // Performance ruler and pose timeline.
    val performanceCell = 44.dp
    val keyDot = 5.dp
    val poseLabelMinWidth = 84.dp

    // Part tree and raster layers.
    val treeRowGap = 1.dp
    val treeIcon = 15.dp
    val treeSmallIcon = 14.dp
    val rasterIndent = 24.dp

    // Cards, previews and dialogs.
    val toolButton = 40.dp
    val presetCard = 96.dp
    val presetIconButton = 28.dp
    val presetVectorIcon = 64.dp
    val assetCardWidth = 144.dp
    val assetCardThumbHeight = 104.dp
    val actorCardThumbHeight = 112.dp
    val detailsThumb = 88.dp
    val detailsListMaxHeight = 360.dp
    val templateThumb = 52.dp
    val templateThumbInset = 5.dp
    val blueprintStroke = 2.dp
    val blueprintJoint = 2.dp
    val coverageBarHeight = 3.dp
    val artworkPickerMaxHeight = 420.dp
    val artworkCell = 88.dp
    val artworkThumbHeight = 72.dp
    val alignmentListMaxHeight = 320.dp
    val alignmentPreviewHeight = 200.dp
    val panelMinMargin = 24.dp
    val sketchHintStartInset = 76.dp
    val sketchToolRailMaxHeight = 260.dp
}
