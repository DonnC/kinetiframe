package com.donnclab.kinetiframe.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object ActorStudioTokens {
    val panelSurface = NeutralNightSurface
    val panelSurfaceVariant = NeutralNightHigh
    val panelDivider = NeutralInkVariant
    val onPanel = BrandPaper
    val onPanelMuted = NeutralNightOnVariant

    // Handles, selected parts and binding marks use the brand carmine, as selection does in the
    // editor; it reads on the stage's paper and on this always-dark panel.
    val accent = BrandCarmine

    /** Content on [accent], such as an enabled apply icon. */
    val onAccent = Color.White

    /** The sketch editor's binding region and its outline. */
    val sketchBind = BrandCarmine
    val partRailIdle = NeutralPaperHigh
    val presetIconStroke = BrandGraphite
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

    /** A finger-sized ring at a chain tip while animating; see-through so the drawing shows. */
    val poseHandleRadius = 18.dp
    const val POSE_HANDLE_FILL_ALPHA = 0.18f

    /** How much of the canvas an actor fills when Actor Studio opens it. */
    const val ACTOR_FRAME_FILL = 0.88f

    /** Narrower than this, the selected part's tools stack above the part rail. */
    val partBarStackBreakpoint = 600.dp

    /** The share of the canvas height the part rail and part tools cover at the bottom. */
    const val ACTOR_FRAME_BOTTOM_INSET = 0.24f

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
