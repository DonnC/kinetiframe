// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.contracts

import com.donnclab.kinetiframe.core.contracts.backup.BackupDownload
import com.donnclab.kinetiframe.core.contracts.backup.BackupEntry
import com.donnclab.kinetiframe.core.contracts.backup.BackupPageRequest
import com.donnclab.kinetiframe.core.contracts.backup.BackupTransfer
import com.donnclab.kinetiframe.core.contracts.backup.BackupUpload
import com.donnclab.kinetiframe.core.contracts.backup.BinaryDestination
import com.donnclab.kinetiframe.core.contracts.backup.BinarySource
import com.donnclab.kinetiframe.core.contracts.backup.DisabledBackupStore
import com.donnclab.kinetiframe.core.contracts.backup.SnapshotDescriptor
import com.donnclab.kinetiframe.core.contracts.backup.SnapshotKey
import com.donnclab.kinetiframe.core.contracts.catalog.CatalogQuery
import com.donnclab.kinetiframe.core.contracts.catalog.DisabledAssetCatalog
import com.donnclab.kinetiframe.core.contracts.observability.CrashIncident
import com.donnclab.kinetiframe.core.contracts.observability.NoOpAnalytics
import com.donnclab.kinetiframe.core.contracts.observability.NoOpCrashReporter
import com.donnclab.kinetiframe.core.contracts.observability.NoOpDiagnostics
import com.donnclab.kinetiframe.core.contracts.observability.OperationOutcome
import com.donnclab.kinetiframe.core.contracts.observability.PerformanceSample
import com.donnclab.kinetiframe.core.contracts.observability.UsageEvent
import com.donnclab.kinetiframe.core.contracts.observability.WorkflowOperation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class DisabledServicesTest {
    @Test fun `disabled backup never opens streams or pretends persistence succeeded`() = runTest {
        val entry = BackupEntry(SnapshotKey("fixture"), SnapshotDescriptor(0, "0".repeat(64), 1), 0)
        val upload = BackupUpload(entry, BinarySource { error("must not open") })
        val download = BackupDownload(entry.key, BinaryDestination { error("must not open") })
        assertEquals(BackupTransfer.Failed(ServiceFailure.UNAVAILABLE), DisabledBackupStore.upload(upload).first())
        assertEquals(BackupTransfer.Failed(ServiceFailure.UNAVAILABLE), DisabledBackupStore.download(download).first())
        assertEquals(ServiceResult.Failure(ServiceFailure.UNAVAILABLE), DisabledBackupStore.list(BackupPageRequest()))
        assertEquals(ServiceResult.Failure(ServiceFailure.UNAVAILABLE), DisabledBackupStore.delete(entry.key))
    }

    @Test fun `disabled catalogue reports unavailable`() = runTest {
        assertEquals(ServiceResult.Failure(ServiceFailure.UNAVAILABLE), DisabledAssetCatalog.page(CatalogQuery()))
    }

    @Test fun `no-op telemetry remains callable without any account or SDK`() = runTest {
        NoOpAnalytics.record(UsageEvent(WorkflowOperation.RIGGING, OperationOutcome.SUCCEEDED))
        NoOpDiagnostics.record(PerformanceSample(WorkflowOperation.RIGGING, 1))
        NoOpCrashReporter.report(CrashIncident(WorkflowOperation.RIGGING, ServiceFailure.INVALID_DATA))
    }

    @Test fun `request validation prevents unbounded pages and malformed manifests`() {
        assertThrows(IllegalArgumentException::class.java) { BackupPageRequest(limit = 101) }
        assertThrows(IllegalArgumentException::class.java) { CatalogQuery(limit = 0) }
        assertThrows(IllegalArgumentException::class.java) { SnapshotDescriptor(-1, "0".repeat(64), 1) }
        assertThrows(IllegalArgumentException::class.java) { SnapshotDescriptor(1, "invalid", 1) }
    }
}
