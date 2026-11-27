// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.contracts.backup

import java.io.InputStream
import java.io.OutputStream

/** Re-openable stream, never a whole-project byte array. The adapter closes streams it opens. */
fun interface BinarySource {
    fun open(): InputStream
}

/** Destination owned by the caller; the adapter closes its opened stream on success/cancellation. */
fun interface BinaryDestination {
    fun open(): OutputStream
}

@JvmInline value class SnapshotKey(val value: String) {
    init {
        require(value.isNotBlank())
    }
}
data class SnapshotDescriptor(val bytes: Long, val sha256: String, val schemaVersion: Int) {
    init {
        require(bytes >= 0 && schemaVersion > 0)
        require(sha256.length == SHA256_LENGTH && sha256.all { it.digitToIntOrNull(16) != null })
    }
    private companion object {
        const val SHA256_LENGTH = 64
    }
}
data class BackupEntry(val key: SnapshotKey, val descriptor: SnapshotDescriptor, val createdAtEpochMillis: Long)
data class BackupUpload(val entry: BackupEntry, val source: BinarySource)
data class BackupDownload(val key: SnapshotKey, val destination: BinaryDestination)
