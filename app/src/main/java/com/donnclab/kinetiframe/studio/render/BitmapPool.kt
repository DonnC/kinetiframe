package com.donnclab.kinetiframe.studio.render

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import java.util.concurrent.ConcurrentLinkedQueue

/** Reuses stage-sized bitmaps for drawing and frame composition. The owner calls [clear] once. */
internal class BitmapPool {
    private val pool = ConcurrentLinkedQueue<Bitmap>()

    /** A cleared bitmap of the given size, reused from the pool when one matches. */
    fun acquire(width: Int, height: Int): Bitmap {
        val pooled = pool.poll()
        return if (pooled != null && pooled.width == width && pooled.height == height && !pooled.isRecycled) {
            pooled.eraseColor(Color.TRANSPARENT)
            pooled
        } else {
            createBitmap(width, height)
        }
    }

    fun release(bitmap: Bitmap?) {
        if (bitmap != null && !bitmap.isRecycled) {
            pool.offer(bitmap)
        }
    }

    /** Recycles every pooled bitmap. Called when the studio closes. */
    fun clear() {
        while (true) {
            val bitmap = pool.poll() ?: break
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }
}
