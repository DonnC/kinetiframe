package com.donnclab.kinetiframe.actors.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/** Decode only the chosen item, not a whole pack. Bound allocation before PNG decompression. */
object ActorArtworkDecoder {
    fun decode(file: File, preservePadding: Boolean = false): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Could not read artwork" }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
        val decoded = requireNotNull(
            BitmapFactory.decodeFile(
                file.path,
                BitmapFactory.Options().apply {
                    inSampleSize =
                        sample
                },
            ),
        ) { "Could not decode artwork" }
        return if (preservePadding) decoded else trimTransparentPadding(decoded)
    }

    /** Pack drawings often occupy a tiny portion of their frame. Fit the actual art, not blank canvas. */
    fun trimTransparentPadding(bitmap: Bitmap): Bitmap {
        val row = IntArray(bitmap.width)
        var left = bitmap.width
        var top = bitmap.height
        var right = -1
        var bottom = -1
        for (y in 0 until bitmap.height) {
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            row.forEachIndexed { x, color ->
                if (color ushr 24 != 0) {
                    left = minOf(left, x)
                    right = maxOf(right, x)
                    top = minOf(top, y)
                    bottom = y
                }
            }
        }
        if (right < left) {
            bitmap.recycle()
            error("Artwork is empty")
        }
        if (left == 0 && top == 0 && right == bitmap.width - 1 && bottom == bitmap.height - 1) return bitmap
        return Bitmap.createBitmap(bitmap, left, top, right - left + 1, bottom - top + 1).also {
            if (it !==
                bitmap
            ) {
                bitmap.recycle()
            }
        }
    }
}
