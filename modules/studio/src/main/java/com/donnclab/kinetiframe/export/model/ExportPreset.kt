package com.donnclab.kinetiframe.export.model

import com.donnclab.kinetiframe.core.document.StageDimensions

/**
 * A platform target chosen at export time, independent of the project's own working canvas.
 * [dimensions] null means "same as project" -- the output is encoded at the project's own stage
 * size with no scaling step at all. A non-null value is a *destination* resolution only: frames
 * are letterboxed to fit it (see `ProjectExporter`'s scaling step), the project's canvas and its
 * dashboard card are never touched by this choice. The screen names each target in its own
 * string resources.
 */
enum class ExportPreset(val dimensions: StageDimensions?) {
    SAME_AS_PROJECT(null),
    SQUARE(StageDimensions.Square),
    VERTICAL(StageDimensions.Vertical),
    LANDSCAPE(StageDimensions.LandscapeHd),
}
