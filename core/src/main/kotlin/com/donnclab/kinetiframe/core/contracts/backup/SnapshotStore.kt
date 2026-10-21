// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.contracts.backup

import com.donnclab.kinetiframe.core.contracts.ServiceResult

/** Local immutable snapshot boundary used by backup orchestration, not the mutable editor cache.
 * Publish must leave the previous committed snapshot intact on failure/cancellation. A changed
 * digest requires a new key. Returned sources stream durable bytes and must be reopenable. */
interface SnapshotStore {
    suspend fun load(key: SnapshotKey): ServiceResult<BackupUpload>
    suspend fun publish(snapshot: BackupUpload): ServiceResult<BackupEntry>
    suspend fun delete(key: SnapshotKey): ServiceResult<Unit>
}
