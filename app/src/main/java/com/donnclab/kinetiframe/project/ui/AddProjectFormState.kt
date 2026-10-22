package com.donnclab.kinetiframe.project.ui

/** Everything New Project's form screen collects, bundled into one value so
 *  [AddProjectScreen] and its host never need more than one parameter to read or update it.
 *  Export format isn't part of this form because
 *  [com.donnclab.kinetiframe.export.data.ProjectExporter] only produces MP4. */
data class AddProjectFormState(
    val name: String = "",
    val canvasSelection: CanvasSizeSelection = CanvasSizeSelection(),
    val fps: Int = 12,
)

/** Callbacks [AddProjectScreen] needs, bundled so the composable itself takes state + one
 *  callbacks value rather than four separate lambda parameters. */
data class AddProjectCallbacks(
    val onStateChange: (AddProjectFormState) -> Unit,
    val onOpenCanvasSize: () -> Unit,
    val onBack: () -> Unit,
    val onCreate: () -> Unit,
)
