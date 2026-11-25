package com.donnclab.kinetiframe.presentation.project

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.presentation.R

/**
 * A canvas size a new project can start from. Sizes are a fixed table of real-world shapes and
 * export targets, never derived from this device's screen: a portrait phone can start a landscape
 * YouTube project as easily as a tablet can.
 *
 * [platform] is a product name ("YouTube (1080p)") and stays as written in every language. The
 * shape presets and [CanvasPresets.Custom] take their names from string resources ([nameRes]).
 * Only [isCustom] has editable dimensions.
 */
data class CanvasPreset(
    val platform: String?,
    val dimensions: StageDimensions,
    @StringRes val nameRes: Int? = null,
    val isCustom: Boolean = false,
)

@Composable
fun CanvasPreset.displayName(): String = platform ?: stringResource(requireNotNull(nameRes))

object CanvasPresets {
    val Landscape = CanvasPreset(null, StageDimensions(1920, 1080), R.string.canvas_preset_landscape)
    val Portrait = CanvasPreset(null, StageDimensions(1080, 1920), R.string.canvas_preset_portrait)
    val Square = CanvasPreset(null, StageDimensions(1080, 1080), R.string.canvas_preset_square)

    /** A size the animator types in, starting from full HD landscape. */
    val Custom = CanvasPreset(null, StageDimensions(1920, 1080), R.string.canvas_preset_custom, isCustom = true)

    /** The picture cards on New Project: the three common shapes and Custom. */
    val shapes: List<CanvasPreset> = listOf(Landscape, Portrait, Square, Custom)

    /** The full list in the More sizes sheet: Custom, then each platform's upload size. */
    val all: List<CanvasPreset> = listOf(
        Custom,
        CanvasPreset("YouTube (1080p)", StageDimensions(1920, 1080)),
        CanvasPreset("YouTube (720p)", StageDimensions(1280, 720)),
        CanvasPreset("Instagram (16:9)", StageDimensions(1280, 720)),
        CanvasPreset("Instagram (1:1)", StageDimensions(720, 720)),
        CanvasPreset("TikTok (1080p)", StageDimensions(1080, 1920)),
        CanvasPreset("TikTok (720p)", StageDimensions(720, 1280)),
        CanvasPreset("Vimeo (1080p)", StageDimensions(1920, 1080)),
        CanvasPreset("Facebook (720p)", StageDimensions(1280, 720)),
        CanvasPreset("Tumblr (16:9)", StageDimensions(540, 304)),
        CanvasPreset("Tumblr (4:3)", StageDimensions(540, 405)),
    )
}

/** The New Project screen's whole canvas-size choice, bundled into one value so
 *  AddProjectScreen/CanvasSizeSheet never need more than one parameter to pass it
 *  around. [dimensions] is what actually gets passed to [com.donnclab.kinetiframe.presentation.project.ProjectManagerIntent.Create] --
 *  the edited [customDimensions] when [CanvasPreset.isCustom], the preset's own fixed size
 *  otherwise. */
data class CanvasSizeSelection(
    val preset: CanvasPreset = CanvasPresets.Landscape,
    val customDimensions: StageDimensions = CanvasPresets.Custom.dimensions,
) {
    val dimensions: StageDimensions get() = if (preset.isCustom) customDimensions else preset.dimensions
}
