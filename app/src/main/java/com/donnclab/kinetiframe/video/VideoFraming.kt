package com.donnclab.kinetiframe.video

import com.donnclab.kinetiframe.project.model.VideoClipPlacement
import com.donnclab.kinetiframe.project.model.VideoFit

/** Pure, normalized crop math shared by native preview and export; no bitmap allocations. */
object VideoFraming {
    data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)
    data class Framing(val source: Rect, val destination: Rect)
    fun resolve(clip: VideoClipPlacement, sourceWidth: Int, sourceHeight: Int): Framing {
        require(sourceWidth > 0 && sourceHeight > 0)
        val sw = sourceWidth.toFloat()
        val sh = sourceHeight.toFloat()
        var source = Rect(0f, 0f, sw, sh)
        var target = Rect(clip.x, clip.y, clip.x + clip.width, clip.y + clip.height)
        when (clip.fit) {
            VideoFit.STRETCH -> Unit

            VideoFit.FIT -> {
                val scale = minOf(clip.width / sw, clip.height / sh)
                val w = sw * scale
                val h = sh * scale
                target = Rect(
                    clip.x + (clip.width - w) / 2,
                    clip.y + (clip.height - h) / 2,
                    clip.x + (clip.width + w) / 2,
                    clip.y + (clip.height + h) / 2,
                )
            }

            VideoFit.FILL -> {
                val scale = maxOf(clip.width / sw, clip.height / sh)
                val w = clip.width / scale
                val h = clip.height / scale
                val x = (sw - w) * clip.cropX
                val y = (sh - h) * clip.cropY
                source = Rect(x, y, x + w, y + h)
            }
        }
        return Framing(source, target)
    }
}
