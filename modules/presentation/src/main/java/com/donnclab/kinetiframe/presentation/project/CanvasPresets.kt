package com.donnclab.kinetiframe.presentation.project

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.presentation.R

/**
 * A named canvas size a new project can start from. Deliberately a fixed table of real-world
 * export targets (matching the sizes FlipaClip itself ships) rather than anything derived from
 * the device's own screen or orientation -- a portrait phone should be just as able to start a
 * landscape YouTube project as a landscape tablet is, so nothing here reads display metrics.
 *
 * [CanvasPresets.Local] is the odd one out: it is the only preset whose dimensions the animator
 * can edit directly on the canvas-size screen (see AddProjectCanvasSizeScreen), for a project
 * meant to stay on this device rather than target one specific platform's upload spec.
 *
 * [platform] is a product name ("YouTube (1080p)") and stays as written in every language. It is
 * null for [CanvasPresets.Local], whose name comes from string resources; see [displayName].
 */
data class CanvasPreset(val platform: String?, val dimensions: StageDimensions) {
    val isLocal: Boolean get() = platform == null
}

@Composable
fun CanvasPreset.displayName(): String = platform ?: stringResource(R.string.canvas_preset_local)

object CanvasPresets {
    /** Default local export target -- same pixel size as YouTube's 1080p preset, since that is
     *  the most broadly compatible landscape HD size a locally-kept file could want. */
    val Local = CanvasPreset(platform = null, StageDimensions(1920, 1080))

    val all: List<CanvasPreset> = listOf(
        Local,
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
 *  AddProjectScreen/AddProjectCanvasSizeScreen never need more than one parameter to pass it
 *  around. [dimensions] is what actually gets passed to [com.donnclab.kinetiframe.presentation.project.ProjectManagerIntent.Create] --
 *  the edited [localDimensions] when [CanvasPreset.isLocal], the preset's own fixed size
 *  otherwise. */
data class CanvasSizeSelection(
    val preset: CanvasPreset = CanvasPresets.Local,
    val localDimensions: StageDimensions = CanvasPresets.Local.dimensions,
) {
    val dimensions: StageDimensions get() = if (preset.isLocal) localDimensions else preset.dimensions
}
