package com.donnclab.kinetiframe.media.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/** Decode only a bounded preview; the original project file remains unchanged for export. */
object BackgroundImageDecoder {
    fun decode(file: File): Bitmap {
        require(file.isFile) { "Saved background image is missing. Replace it from the image menu." }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        require(options.outWidth > 0 && options.outHeight > 0) { "Saved background image could not be decoded." }
        options.inSampleSize = backgroundSampleSize(options.outWidth, options.outHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        return requireNotNull(BitmapFactory.decodeFile(file.absolutePath, options)) {
            "Saved background image could not be decoded."
        }
    }
}
