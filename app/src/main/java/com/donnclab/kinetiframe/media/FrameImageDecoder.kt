package com.donnclab.kinetiframe.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.InputStream

/** Probe first: a phone camera image must not allocate tens of megapixels to stamp a frame. */
object FrameImageDecoder {
    fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        require(width > 0 && height > 0 && maxEdge > 0)
        var sample = 1
        while (maxOf(width, height) / sample > maxEdge) sample *= 2
        return sample
    }

    fun decode(maxEdge: Int = 2048, open: () -> InputStream?): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val probe = open() ?: return null
        probe.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return open()?.use { BitmapFactory.decodeStream(it, null, options) }
    }
}
