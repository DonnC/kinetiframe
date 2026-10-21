package com.donnclab.kinetiframe.ui.color

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** PixelCopy includes the rendered canvas, imported assets and app chrome without a second renderer. */
class WindowColorSource(private val window: Window) : ScreenColorSource {
    override suspend fun capture(): ScreenColorSnapshot {
        val decor = requireNotNull(window.peekDecorView())
        val sourceWidth = decor.width
        val sourceHeight = decor.height
        val (width, height) = screenCaptureSize(sourceWidth, sourceHeight)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        return suspendCancellableCoroutine { continuation ->
            try {
                // The native copy cannot be cancelled. Recycling while it writes would race;
                // its completion callback disposes the bitmap if the coroutine was cancelled.
                PixelCopy.request(window, Rect(0, 0, sourceWidth, sourceHeight), bitmap, { result ->
                    if (!continuation.isActive || result != PixelCopy.SUCCESS) {
                        bitmap.recycle()
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                IOException("Screen capture failed ($result)"),
                            )
                        }
                    } else {
                        continuation.resume(BitmapScreenColors(bitmap, sourceWidth, sourceHeight)) { _, value, _ ->
                            value.close()
                        }
                    }
                }, Handler(Looper.getMainLooper()))
            } catch (error: Exception) {
                bitmap.recycle()
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }
    }
}

private class BitmapScreenColors(
    private val bitmap: Bitmap,
    override val sourceWidth: Int,
    override val sourceHeight: Int,
) : ScreenColorSnapshot {
    override val width: Int get() = bitmap.width
    override val height: Int get() = bitmap.height
    override fun pixel(x: Int, y: Int): Int = bitmap.getPixel(x, y)

    // Snapshot is sampled only, never submitted to Compose/GPU drawing, so recycling is safe.
    override fun close() {
        if (!bitmap.isRecycled) bitmap.recycle()
    }
}
