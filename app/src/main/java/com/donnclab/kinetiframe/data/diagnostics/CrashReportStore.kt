package com.donnclab.kinetiframe.data.diagnostics

import com.donnclab.kinetiframe.data.storage.DurableFile
import java.io.File

/** One bounded, private report. No artwork, audio, document JSON, or credentials are collected. */
class CrashReportStore(private val filesDir: File) {
    val reportFile: File get() = File(filesDir, "diagnostics/last-error.txt")

    fun record(error: Throwable, operation: String) {
        DurableFile.write(reportFile) { output ->
            val writer = output.bufferedWriter()
            writer.appendLine("KinetiFrame diagnostic | ${System.currentTimeMillis()}")
            writer.appendLine(operation.take(200))
            var cause: Throwable? = error
            repeat(6) {
                val current = cause ?: return@repeat
                writer.appendLine("${current.javaClass.name}: ${current.message?.take(500).orEmpty()}")
                current.stackTrace.take(24).forEach { frame -> writer.appendLine("  at ${frame.toString().take(500)}") }
                cause = current.cause.takeUnless { it === current }
            }
            writer.flush()
        }
    }
}
