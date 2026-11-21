// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.contracts.backup

import com.donnclab.kinetiframe.core.contracts.ServiceFailure
import com.donnclab.kinetiframe.core.contracts.ServiceResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

enum class BackupCapability { DELETE, RESUMABLE_TRANSFER }
data class BackupCapabilities(val maximumBytes: Long, val supported: Set<BackupCapability>)
data class BackupPageRequest(val cursor: String? = null, val limit: Int = DEFAULT_LIMIT) {
    init {
        require(limit in 1..MAXIMUM_LIMIT)
    }
    companion object {
        const val DEFAULT_LIMIT = 32
        const val MAXIMUM_LIMIT = 100
    }
}
data class BackupPage(val entries: List<BackupEntry>, val nextCursor: String?)
sealed interface BackupTransfer {
    data class Progress(val bytes: Long, val total: Long) : BackupTransfer
    data class Complete(val entry: BackupEntry) : BackupTransfer
    data class Failed(val reason: ServiceFailure) : BackupTransfer
}

/** Cold single-operation flows. Same key+digest must be idempotent; changed bytes need a new key.
 * Completion follows checksum validation/publication, never an optimistic UI acknowledgement.
 * Restore policy validates the native archive after download; this port does not implement sync. */
interface RemoteBackupStore {
    val capabilities: BackupCapabilities
    suspend fun list(request: BackupPageRequest): ServiceResult<BackupPage>
    fun upload(request: BackupUpload): Flow<BackupTransfer>
    fun download(request: BackupDownload): Flow<BackupTransfer>
    suspend fun delete(key: SnapshotKey): ServiceResult<Unit>
}

/** Explicitly unavailable, never a fake successful cloud save or silently opened data stream. */
object DisabledBackupStore : RemoteBackupStore {
    override val capabilities = BackupCapabilities(0, emptySet())
    override suspend fun list(request: BackupPageRequest) = ServiceResult.Failure(ServiceFailure.UNAVAILABLE)
    override fun upload(request: BackupUpload) = flowOf(BackupTransfer.Failed(ServiceFailure.UNAVAILABLE))
    override fun download(request: BackupDownload) = flowOf(BackupTransfer.Failed(ServiceFailure.UNAVAILABLE))
    override suspend fun delete(key: SnapshotKey) = ServiceResult.Failure(ServiceFailure.UNAVAILABLE)
}
