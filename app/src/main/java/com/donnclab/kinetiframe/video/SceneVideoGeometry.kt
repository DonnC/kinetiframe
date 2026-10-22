package com.donnclab.kinetiframe.video

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.donnclab.kinetiframe.project.model.VideoClipPlacement

/** Stage geometry shared by lasso hit-testing, live preview, and export. */
object SceneVideoGeometry {
    fun corners(clip: VideoClipPlacement): List<Offset> = listOf(
        Offset(clip.x, clip.y),
        Offset(clip.x + clip.width, clip.y),
        Offset(clip.x + clip.width, clip.y + clip.height),
        Offset(clip.x, clip.y + clip.height),
    ).map { point ->
        Offset(clip.selectionTransform.mapX(point.x, point.y), clip.selectionTransform.mapY(point.x, point.y))
    }
    fun bounds(clip: VideoClipPlacement): Rect = corners(clip).let { points ->
        Rect(points.minOf { it.x }, points.minOf { it.y }, points.maxOf { it.x }, points.maxOf { it.y })
    }
}
