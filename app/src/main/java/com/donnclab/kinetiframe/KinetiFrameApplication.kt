package com.donnclab.kinetiframe

import android.app.Application
import com.donnclab.kinetiframe.diagnostics.CrashReportStore

class KinetiFrameApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val reports = CrashReportStore(filesDir)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            // Best effort, including memory pressure. Never swallow a fatal error or replace
            // Android's normal termination/reporting. OS kills/native faults bypass this hook.
            try {
                reports.record(error, "Uncaught failure on ${thread.name}")
            } catch (_: Throwable) { }
            previous?.uncaughtException(thread, error)
        }
    }
}
