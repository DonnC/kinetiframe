package com.donnclab.kinetiframe

import android.app.Application

class KinetiFrameApplication : Application() {
    /** The app's shared services, created on first use and kept for the life of the process. */
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        val reports = container.crashReports
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
