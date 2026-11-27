package com.donnclab.kinetiframe.media.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.donnclab.kinetiframe.core.contracts.ActionableException
import com.donnclab.kinetiframe.core.contracts.ActionableProblem
import java.io.File

/** Decode only a bounded preview; the original project file remains unchanged for export. */
object BackgroundImageDecoder {
    fun decode(file: File): Bitmap {
        if (!file.isFile) throw ActionableException(ActionableProblem.BACKGROUND_MISSING)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        if (options.outWidth <= 0 ||
            options.outHeight <= 0
        ) {
            throw ActionableException(ActionableProblem.BACKGROUND_UNREADABLE)
        }
        options.inSampleSize = backgroundSampleSize(options.outWidth, options.outHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        return BitmapFactory.decodeFile(file.absolutePath, options)
            ?: throw ActionableException(ActionableProblem.BACKGROUND_UNREADABLE)
    }
}
