// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.data.storage

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * The app-private folder that holds actors, projects and the library, with atomic file
 * operations. Repositories validate the documents they store here.
 */
class DocumentFileStore(private val root: File) {
    init {
        root.mkdirs()
    }

    fun actorsRoot() = File(root, "actors").apply(File::mkdirs)
    fun projectsRoot() = File(root, "projects").apply(File::mkdirs)
    fun libraryRoot() = File(root, "library").apply(File::mkdirs)
    private fun checkedId(id: String): String = id.also { StorageIds.requireValid(it, "storage ID") }
    fun actorDirectory(actorId: String) = File(root, "actors/${checkedId(actorId)}")
    fun projectDirectory(projectId: String) = File(root, "projects/${checkedId(projectId)}")
    fun performanceFile(projectId: String, performanceId: String) =
        File(projectDirectory(projectId), "performances/${checkedId(performanceId)}.json")
    fun archivedPerformanceFile(projectId: String, performanceId: String) =
        File(projectDirectory(projectId), "archive/performances/${checkedId(performanceId)}.json")
    fun libraryDirectory(itemId: String) = File(root, "library/${checkedId(itemId)}")

    /** A project-scoped library item lives inside its project's folder, so `.kframe` exports carry it. */
    fun projectVaultRoot(projectId: String) = File(projectDirectory(projectId), "vault").apply(File::mkdirs)
    fun projectVaultDirectory(projectId: String, itemId: String) =
        File(projectDirectory(projectId), "vault/${checkedId(itemId)}")
    fun archiveDirectory(actorId: String) = File(root, "archive/actors/${checkedId(actorId)}")

    fun writeAtomically(target: File, content: String) {
        DurableFile.write(target) {
            it.write(content.toByteArray(Charsets.UTF_8))
        }
    }

    fun readOrNull(file: File): String? = file.takeIf(File::isFile)?.readText()

    fun move(source: File, target: File) {
        target.parentFile?.mkdirs()
        try {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
