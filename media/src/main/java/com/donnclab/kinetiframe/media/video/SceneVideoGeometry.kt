package com.donnclab.kinetiframe.media.video

import com.donnclab.kinetiframe.core.document.VideoClipPlacement
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.core.geometry.Rect2

/** Stage geometry shared by lasso hit-testing, live preview, and export. */
object SceneVideoGeometry {
    fun corners(clip: VideoClipPlacement): List<Point2> = listOf(
        Point2(clip.x, clip.y),
        Point2(clip.x + clip.width, clip.y),
        Point2(clip.x + clip.width, clip.y + clip.height),
        Point2(clip.x, clip.y + clip.height),
    ).map { point ->
        Point2(clip.selectionTransform.mapX(point.x, point.y), clip.selectionTransform.mapY(point.x, point.y))
    }
    fun bounds(clip: VideoClipPlacement): Rect2 = corners(clip).let { points ->
        Rect2(points.minOf { it.x }, points.minOf { it.y }, points.maxOf { it.x }, points.maxOf { it.y })
    }
}
