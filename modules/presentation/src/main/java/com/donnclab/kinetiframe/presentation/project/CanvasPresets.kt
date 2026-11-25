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

    // Where short animations are posted most, by each platform's recommended upload size (2026).
    // 9:16 also suits Instagram Reels and YouTube Shorts; 4:5 also suits Facebook's feed.
    val TikTok = CanvasPreset("TikTok", StageDimensions(1080, 1920))
    val InstagramPost = CanvasPreset("Instagram", StageDimensions(1080, 1350))
    val YouTube = CanvasPreset("YouTube", StageDimensions(1920, 1080))
    val Facebook = CanvasPreset("Facebook", StageDimensions(1080, 1080))

    /** The picture cards on New Project: the four places animators post most. Custom is in More sizes. */
    val shapes: List<CanvasPreset> = listOf(TikTok, InstagramPost, YouTube, Facebook)

    /** The full list in the More sizes sheet: Custom, the plain shapes, then each platform's sizes. */
    val all: List<CanvasPreset> = listOf(
        Custom,
        Landscape,
        Portrait,
        Square,
        CanvasPreset("TikTok (1080p)", StageDimensions(1080, 1920)),
        CanvasPreset("TikTok (720p)", StageDimensions(720, 1280)),
        CanvasPreset("Instagram Reels and Stories", StageDimensions(1080, 1920)),
        CanvasPreset("Instagram post (4:5)", StageDimensions(1080, 1350)),
        CanvasPreset("Instagram grid (3:4)", StageDimensions(1080, 1440)),
        CanvasPreset("Instagram square (1:1)", StageDimensions(1080, 1080)),
        CanvasPreset("YouTube (1080p)", StageDimensions(1920, 1080)),
        CanvasPreset("YouTube (720p)", StageDimensions(1280, 720)),
        CanvasPreset("YouTube Shorts", StageDimensions(1080, 1920)),
        CanvasPreset("Facebook Reels", StageDimensions(1080, 1920)),
        CanvasPreset("Facebook feed (4:5)", StageDimensions(1080, 1350)),
        CanvasPreset("Facebook square (1:1)", StageDimensions(1080, 1080)),
        CanvasPreset("Vimeo (1080p)", StageDimensions(1920, 1080)),
        CanvasPreset("Tumblr (16:9)", StageDimensions(540, 304)),
        CanvasPreset("Tumblr (4:3)", StageDimensions(540, 405)),
    )
}

/** The shape as a ratio such as "9:16" or "4:5". */
fun StageDimensions.ratioLabel(): String {
    tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
    val divisor = gcd(width, height).coerceAtLeast(1)
    return "${width / divisor}:${height / divisor}"
}

/** The New Project screen's whole canvas-size choice, bundled into one value so
 *  AddProjectScreen/CanvasSizeSheet never need more than one parameter to pass it
 *  around. [dimensions] is what actually gets passed to [com.donnclab.kinetiframe.presentation.project.ProjectManagerIntent.Create] --
 *  the edited [customDimensions] when [CanvasPreset.isCustom], the preset's own fixed size
 *  otherwise. */
data class CanvasSizeSelection(
    // Vertical video is what most phone animators post, so a new project starts there.
    val preset: CanvasPreset = CanvasPresets.TikTok,
    val customDimensions: StageDimensions = CanvasPresets.Custom.dimensions,
) {
    val dimensions: StageDimensions get() = if (preset.isCustom) customDimensions else preset.dimensions
}
