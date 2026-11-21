package com.donnclab.kinetiframe.presentation.common

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Offers [file] to other apps through the system share sheet, titled [title]. The file must sit
 * under a path the app's FileProvider shares. Call with an Activity's context: the share sheet is
 * an activity, and starting one from the application context throws.
 */
fun shareFile(context: Context, file: File, title: String, mimeType: String = "application/octet-stream") {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, title))
}
