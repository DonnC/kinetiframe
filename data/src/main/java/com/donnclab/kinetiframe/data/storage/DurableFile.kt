package com.donnclab.kinetiframe.data.storage

import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Publish only closed, synced bytes. Unique siblings prevent concurrent writers sharing a temp. */
object DurableFile {
    // Bounded stripes avoid retaining a lock for every resource ever saved. Sharing them across
    // repository instances also prevents overlapping Windows replace operations on one path.
    private val publicationLocks = Array(64) { Any() }

    fun write(target: File, writer: (FileOutputStream) -> Unit) {
        val lock = publicationLocks[(target.absoluteFile.normalize().path.hashCode() ushr 1) % publicationLocks.size]
        synchronized(lock) {
            val parent = requireNotNull(target.absoluteFile.parentFile)
            check(parent.isDirectory || parent.mkdirs() || parent.isDirectory) {
                "Cannot create storage directory: $parent"
            }
            val temporary = File.createTempFile(".commit-", ".tmp", parent)
            try {
                FileOutputStream(temporary).use { output ->
                    writer(output)
                    output.flush()
                    output.fd.sync()
                }
                try {
                    Files.move(
                        temporary.toPath(),
                        target.toPath(),
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            } finally {
                temporary.delete()
            }
        }
    }

    fun copy(source: File, target: File) = write(target) { output ->
        source.inputStream().use { it.copyTo(output) }
    }
}
