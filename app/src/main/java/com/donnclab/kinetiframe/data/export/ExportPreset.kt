package com.donnclab.kinetiframe.data.export

import com.donnclab.kinetiframe.engine.model.StageDimensions

/**
 * A platform target chosen at export time, independent of the project's own working canvas.
 * [dimensions] null means "same as project" -- the output is encoded at the project's own stage
 * size with no scaling step at all. A non-null value is a *destination* resolution only: frames
 * are letterboxed to fit it (see [ProjectExporter]'s scaling step), the project's canvas and its
 * dashboard card are never touched by this choice.
 */
data class ExportPreset(val label: String, val dimensions: StageDimensions?)

object ExportPresets {
    val Presets = listOf(
        ExportPreset("Same as project", dimensions = null),
        ExportPreset("Square (Instagram, Facebook post)", StageDimensions.Square),
        ExportPreset("Vertical (TikTok, Reels, Shorts)", StageDimensions.Vertical),
        ExportPreset("Landscape (YouTube, Facebook video)", StageDimensions.LandscapeHd),
    )
}
