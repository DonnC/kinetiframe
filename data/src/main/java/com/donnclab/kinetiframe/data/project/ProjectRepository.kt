// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.data.project

import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Project persistence boundary -- mirrors the actor repository's
 * manifest/document split and [StoreResult] error handling exactly, and deliberately reuses
 * [DocumentFileStore]'s existing `projects/<id>/` root: the performance repository
 * already writes a project's performances under that same directory, and the editor points the
 * frame-pixel cache and the persistent background's storage there too, so a project's manifest,
 * document, performances, frame cache and background asset all live under one directory and a
 * project delete is one [File.deleteRecursively] call.
 *
 * `ignoreUnknownKeys = true` (unlike ActorRepository's stricter `false`) is a deliberate deviation:
 * [ProjectSaveDocument] changes more often than the actor formats, and an app that meets a field
 * it doesn't know should still open the project.
 */
class ProjectRepository(
    private val files: DocumentFileStore,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    },
) {
    /** Never returns pack drafts ([ProjectManifest.isPackDraft]) -- they are real project
     *  directories under this same root, but not something a person would open from their gallery. */
    fun list(): StoreResult<List<ProjectManifest>> = runCatching {
        files.projectsRoot().listFiles().orEmpty().filter(File::isDirectory).mapNotNull { directory ->
            runCatching { loadValue(directory.name).first }.getOrNull()
        }.filterNot { it.isPackDraft }.map { manifest ->
            // Derived, not trusted: a manifest written before a thumbnail existed (or one whose
            // file was since deleted) just falls back to null here -- never a stale path a caller
            // would try to decode and fail on.
            val thumbnail = thumbnailFile(manifest.projectId).takeIf(File::exists)?.absolutePath
            if (thumbnail == manifest.thumbnailPath) manifest else manifest.copy(thumbnailPath = thumbnail)
        }.sortedByDescending { it.updatedAtEpochMs }
    }.fold(::value, ::failure)

    /** Where a project's frame-0 preview PNG lives, if one has been generated. It sits inside the
     *  project's directory, so deleting the project removes it too. */
    fun thumbnailFile(projectId: String): File = File(files.projectDirectory(projectId), THUMBNAIL)

    fun isDisplayNameAvailable(displayName: String, excludingProjectId: String? = null): StoreResult<Boolean> =
        runCatching {
            val normalized = displayName.trim().lowercase()
            require(normalized.isNotBlank()) { "Project name cannot be blank" }
            when (val result = list()) {
                is StoreResult.Value -> result.value.none {
                    it.projectId != excludingProjectId &&
                        it.displayName.trim().lowercase() == normalized
                }

                is StoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
            }
        }.fold(::value, ::failure)

    fun save(manifest: ProjectManifest, document: ProjectSaveDocument): StoreResult<ProjectManifest> = runCatching {
        manifest.validate()
        require(manifest.projectId == document.project.id) {
            "Manifest and document disagree on which project this is"
        }
        require(document.totalFrames > 0) { "Project must contain frames" }
        com.donnclab.kinetiframe.core.document.CelExposureIndex.validate(document.linkedCels, document.totalFrames)
        require(manifest.stage == document.project.stage && manifest.fps == document.project.fps) {
            "Project canvas or FPS differs from its manifest"
        }
        val directory = files.projectDirectory(manifest.projectId)
        val committed = manifest.copy(totalFrames = document.totalFrames)
        // Compatibility copies are not the commit record. A restart reads one coherent snapshot,
        // even if the process dies between updating the two legacy files.
        files.writeAtomically(File(directory, DOCUMENT), json.encodeToString(document))
        files.writeAtomically(File(directory, MANIFEST), json.encodeToString(committed))
        files.writeAtomically(File(directory, SNAPSHOT), json.encodeToString(ProjectSnapshot(committed, document)))
        committed
    }.fold(::value, ::failure)

    fun load(projectId: String): StoreResult<Pair<ProjectManifest, ProjectSaveDocument>> = runCatching {
        require(projectId.isNotBlank()) { "Project ID cannot be blank" }
        val directory = files.projectDirectory(projectId)
        files.readOrNull(File(directory, SNAPSHOT))?.let { content ->
            val snapshot = json.decodeFromString<ProjectSnapshot>(content)
            snapshot.manifest.validate()
            require(snapshot.manifest.projectId == projectId && snapshot.document.project.id == projectId) {
                "Project identity mismatch"
            }
            require(
                snapshot.document.totalFrames > 0 && snapshot.manifest.stage == snapshot.document.project.stage &&
                    snapshot.manifest.fps == snapshot.document.project.fps,
            ) { "Invalid project snapshot" }
            com.donnclab.kinetiframe.core.document.CelExposureIndex.validate(
                snapshot.document.linkedCels,
                snapshot.document.totalFrames,
            )
            return@runCatching snapshot.manifest to snapshot.document
        }
        val manifest = json.decodeFromString<ProjectManifest>(
            requireNotNull(files.readOrNull(File(directory, MANIFEST))) {
                "Project not found"
            },
        ).validate()
        val document = json.decodeFromString<ProjectSaveDocument>(
            requireNotNull(files.readOrNull(File(directory, DOCUMENT))) {
                "Project document not found"
            },
        )
        require(document.project.id == manifest.projectId) { "Project document does not match its manifest" }
        com.donnclab.kinetiframe.core.document.CelExposureIndex.validate(document.linkedCels, document.totalFrames)
        manifest to document
    }.fold(::value, ::failure)

    fun rename(projectId: String, displayName: String, nowEpochMs: Long): StoreResult<ProjectManifest> = runCatching {
        val (manifest, document) = loadValue(projectId)
        val renamed = manifest.copy(displayName = displayName.trim(), updatedAtEpochMs = nowEpochMs).validate()
        saveValue(renamed, document)
        renamed
    }.fold(::value, ::failure)

    /**
     * Hard delete: the project's whole directory (manifest, document, performances, frame cache,
     * background asset) is removed in one shot. Unlike archived actors,
     * projects have no recoverable archive, so callers must confirm with the user first. There is
     * also no "in use" guard: nothing else in the app references a project the
     * way a placement references an actor, so archiving-to-protect-referenced-data does not apply here.
     */
    fun delete(projectId: String): StoreResult<Unit> = runCatching {
        require(projectId.isNotBlank()) { "Project ID cannot be blank" }
        val directory = files.projectDirectory(projectId)
        require(directory.isDirectory) { "Project not found" }
        require(directory.deleteRecursively()) { "Could not delete this project's files" }
    }.fold(::value, ::failure)

    private fun loadValue(projectId: String): Pair<ProjectManifest, ProjectSaveDocument> =
        when (val result = load(projectId)) {
            is StoreResult.Value -> result.value
            is StoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
        }

    private fun saveValue(manifest: ProjectManifest, document: ProjectSaveDocument) {
        when (val result = save(manifest, document)) {
            is StoreResult.Value -> Unit
            is StoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
        }
    }

    private fun <T> value(value: T): StoreResult<T> = StoreResult.Value(value)
    private fun failure(error: Throwable): StoreResult.Failure =
        StoreResult.Failure(error.message ?: "Project storage failed", error)
    private companion object {
        const val MANIFEST = "manifest.json"
        const val DOCUMENT = "document.json"
        const val SNAPSHOT = "snapshot.json"
        const val THUMBNAIL = "thumbnail.png"
    }
}

@Serializable
data class ProjectSnapshot(val manifest: ProjectManifest, val document: ProjectSaveDocument)
