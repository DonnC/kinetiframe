// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.data.diagnostics

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CrashReportStoreTest {
    @TempDir lateinit var directory: File

    @Test fun `last error persists across instances and replaces rather than grows`() {
        val first = CrashReportStore(directory)
        first.record(IllegalStateException("decoder failed", java.io.IOException("source failed")), "Playback")
        val reopened = CrashReportStore(directory)
        assertTrue(reopened.reportFile.readText().contains("source failed"))
        reopened.record(IllegalArgumentException("x".repeat(100000)), "next")
        assertTrue(reopened.reportFile.length() < 16000)
        assertFalse(reopened.reportFile.readText().contains("source failed"))
    }
}
