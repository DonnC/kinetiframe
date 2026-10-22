package com.donnclab.kinetiframe.diagnostics

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.donnclab.kinetiframe.diagnostics.CrashReportStore

fun shareDiagnostics(context: Context) {
    val file = CrashReportStore(context.filesDir).reportFile
    if (!file.isFile) {
        Toast.makeText(context, "No recorded app errors", Toast.LENGTH_SHORT).show()
        return
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            "Share KinetiFrame diagnostics",
        ),
    )
}
