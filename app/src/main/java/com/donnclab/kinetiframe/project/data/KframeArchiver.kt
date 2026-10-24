package com.donnclab.kinetiframe.project.data

import com.donnclab.kinetiframe.data.storage.ContainedPaths
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.data.storage.DurableFile
import com.donnclab.kinetiframe.data.storage.StoreResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * The `.kframe` file format, implemented here as an explicit export/import/share *action* rather
 * than a change to how a project is stored while it's being edited. The already-built
 * `projects/<id>/` directory (manifest.json, document.json, performances/, frames/,
 * project-assets/) is the project's real, live working storage -- unzipping it on every open and
 * re-zipping it on every save would mean compressing hundreds of frame PNGs on every single edit,
 * a severe performance regression for no real benefit while a project is open locally. `.kframe`
 * only exists at the boundary where a project leaves or enters this app: tapping "Export" or
 * "Share" zips the working directory as it stands right now; opening a `.kframe` someone sent you
 * unzips it back into a fresh working directory.
 *
 * Import deliberately keeps the archive's own embedded project ID rather than assigning a new
 * one. Every already-serialized [ProjectSaveDocument] and [ProjectManifest] on disk can contain
 * absolute paths (project asset source paths, performance file locations) that were baked in at
 * save time; reassigning the ID on import would mean relocating every referenced file *and*
 * rewriting every embedded path pointing at them -- a large surface for subtle corruption for a
 * problem that, since project IDs are random UUIDs, will almost never actually occur. So instead:
 * a genuine collision is surfaced as a clear, refused import rather than silently overwritten or
 * "fixed" with path-rewriting magic.
 */
object KframeArchiver {
    private val json = Json { ignoreUnknownKeys = true }

    /** Zips every file under [projectId]'s working directory into [destination]. Entry paths are
     *  relative to that directory's root, so re-importing recreates the same manifest.json /
     *  document.json / frames/ / project-assets/ / performances/ layout. */
    fun export(files: DocumentFileStore, projectId: String, destination: File): StoreResult<File> = runCatching {
        val sourceDir = files.projectDirectory(projectId)
        require(sourceDir.isDirectory) { "Project not found" }
        val saved = when (val loaded = ProjectRepository(files).load(projectId)) {
            is StoreResult.Value -> loaded.value
            is StoreResult.Failure -> throw IllegalStateException(loaded.message, loaded.cause)
        }
        val document = saved.second
        require(!destination.canonicalPath.startsWith(sourceDir.canonicalPath + File.separator)) {
            "Archive destination cannot be inside the project"
        }
        document.project.actorPlacements.values.forEach { placement ->
            val revision = when (
                val loaded = com.donnclab.kinetiframe.actors.data.ActorRepository(
                    files,
                ).loadRevision(placement.actorId, placement.actorRevisionId)
            ) {
                is StoreResult.Value -> loaded.value
                is StoreResult.Failure -> throw IllegalStateException(loaded.message, loaded.cause)
            }
            val performance = when (
                val loaded = com.donnclab.kinetiframe.actors.data.PerformanceRepository(
                    files,
                ).load(projectId, placement.performanceId)
            ) {
                is StoreResult.Value -> loaded.value
                is StoreResult.Failure -> throw IllegalStateException(loaded.message, loaded.cause)
            }
            com.donnclab.kinetiframe.actors.domain.PerformanceCompatibility.validate(performance, revision)
        }
        val external = document.project.assets.values.mapNotNull { asset ->
            asset.sourcePath?.let { path ->
                val file = File(path)
                require(file.isFile) { "Project asset is missing: ${asset.id}" }
                if (file.canonicalPath.startsWith(sourceDir.canonicalPath + File.separator)) {
                    null
                } else {
                    path to "portable-assets/${java.util.UUID.randomUUID()}.${file.extension.ifBlank { "bin" }}"
                }
            }
        }.toMap()
        DurableFile.write(destination) { output ->
            ZipOutputStream(object : java.io.FilterOutputStream(output) {
                override fun close() {
                    flush()
                }
            }).use { zip ->
                sourceDir.walkTopDown().filter {
                    it.isFile && !it.name.startsWith(".commit-") &&
                        it.relativeTo(sourceDir).invariantSeparatorsPath !in
                        setOf("manifest.json", "document.json", "snapshot.json", "archive-paths.json") &&
                        !it.relativeTo(sourceDir).invariantSeparatorsPath.startsWith("dependencies/")
                }.forEach { file ->
                    val relativePath = file.relativeTo(sourceDir).path.replace(File.separatorChar, '/')
                    zip.putNextEntry(ZipEntry(relativePath))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
                mapOf(
                    "manifest.json" to json.encodeToString(saved.first),
                    "document.json" to json.encodeToString(document),
                    "snapshot.json" to json.encodeToString(ProjectSnapshot(saved.first, document)),
                ).forEach { (path, content) ->
                    zip.putNextEntry(ZipEntry(path))
                    zip.write(content.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
                external.forEach { (path, relative) -> addFile(zip, File(path), relative) }
                document.project.actorPlacements.values.map { it.actorId }.distinct().forEach { actorId ->
                    val actor = files.actorDirectory(actorId)
                    require(File(actor, "manifest.json").isFile) { "Project actor is missing: $actorId" }
                    actor.walkTopDown().filter { it.isFile && !it.name.startsWith(".commit-") }.forEach {
                        addFile(zip, it, "dependencies/actors/$actorId/${it.relativeTo(actor).invariantSeparatorsPath}")
                    }
                }
                zip.putNextEntry(ZipEntry("archive-paths.json"))
                zip.write(
                    json.encodeToString(
                        ProjectArchivePaths(sourceDir.absolutePath, external),
                    ).toByteArray(Charsets.UTF_8),
                )
                zip.closeEntry()
            }
        }
        destination
    }.fold(::value, ::failure)

    /** Unzips [source] into a fresh project directory, named after the project ID embedded in
     *  its own manifest.json -- never [source]'s filename or a freshly minted ID. Returns that
     *  project ID on success so the caller can immediately open or list the imported project. */
    fun import(files: DocumentFileStore, source: File): StoreResult<String> = runCatching {
        require(source.isFile) { "Archive not found" }
        val stagingDir = File(files.projectsRoot(), "_import-${UUID.randomUUID()}")
        require(stagingDir.mkdirs()) { "Could not prepare a location to unpack this project" }
        try {
            ZipInputStream(source.inputStream().buffered()).use { zip ->
                generateSequence { zip.nextEntry }.forEach { entry ->
                    val outFile = File(stagingDir, entry.name)
                    require(outFile.canonicalPath.startsWith(stagingDir.canonicalPath + File.separator)) {
                        "Archive contains an unsafe file path"
                    }
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                }
            }
            val manifestFile = File(stagingDir, "manifest.json")
            require(manifestFile.isFile) { "This file is not a valid KinetiFrame project (.kframe) archive" }
            var manifest = json.decodeFromString<ProjectManifest>(manifestFile.readText()).validate()
            val finalDir = files.projectDirectory(manifest.projectId)
            require(!finalDir.exists()) { "A project with this ID already exists locally" }
            val paths = File(stagingDir, "archive-paths.json").takeIf(File::isFile)?.let {
                json.decodeFromString<ProjectArchivePaths>(it.readText())
            }
            if (paths != null) {
                require(paths.sourceDirectory.isNotBlank()) { "Archive source path is missing" }
                paths.externalAssets.values.forEach { ContainedPaths.resolveWithin(finalDir, it, "Portable asset") }
                listOf("manifest.json", "document.json", "snapshot.json").forEach { name ->
                    val file = File(stagingDir, name)
                    if (file.isFile) {
                        files.writeAtomically(
                            file,
                            paths.relocate(json.parseToJsonElement(file.readText()), finalDir.absolutePath).toString(),
                        )
                    }
                }
                manifest = json.decodeFromString<ProjectManifest>(manifestFile.readText()).validate()
            }
            // Validate the whole project before publishing its directory, not just its title.
            val document = json.decodeFromString<ProjectSaveDocument>(File(stagingDir, "document.json").readText())
            require(document.project.id == manifest.projectId && document.totalFrames > 0)
            require(document.project.stage == manifest.stage && document.project.fps == manifest.fps)
            com.donnclab.kinetiframe.studio.render.CelExposureIndex.validate(document.linkedCels, document.totalFrames)
            document.linkedCels.forEach { (layer, exposures) ->
                exposures.values.distinct().forEach { cel ->
                    val key = com.donnclab.kinetiframe.studio.render.CelExposureIndex.storageKey(layer, cel)
                    require(File(stagingDir, "frames/frame_v2_$key.png").isFile) { "Held drawing pixels are missing" }
                }
            }
            File(stagingDir, "snapshot.json").takeIf(File::isFile)?.let {
                val snapshot = json.decodeFromString<ProjectSnapshot>(it.readText())
                require(snapshot.document == document && snapshot.manifest == manifest) {
                    "Archive contains conflicting project snapshots"
                }
            }
            val dependencyRepository = com.donnclab.kinetiframe.actors.data.ActorRepository(
                DocumentFileStore(File(stagingDir, "dependencies")),
            )
            File(stagingDir, "dependencies/actors").listFiles().orEmpty().filter(File::isDirectory).forEach { actor ->
                when (val checked = dependencyRepository.load(actor.name)) {
                    is StoreResult.Value -> checked.value.first.authoredViews().values.forEach { revisionId ->
                        val revision = when (val snapshot = dependencyRepository.loadRevision(actor.name, revisionId)) {
                            is StoreResult.Value -> snapshot.value
                            is StoreResult.Failure -> error(snapshot.message)
                        }
                        revision.artworkAssetPaths.values.forEach { path ->
                            require(ContainedPaths.resolveWithin(actor, path, "Actor artwork").isFile) {
                                "Actor dependency artwork is missing"
                            }
                        }
                    }

                    is StoreResult.Failure -> throw IllegalStateException(checked.message, checked.cause)
                }
                val target = files.actorDirectory(actor.name)
                // Immutable revisions must agree semantically; pixel assets must agree byte-for-byte.
                // Never overwrite either matching payloads or the user's current actor catalog.
                actor.walkTopDown().filter(File::isFile).forEach { sourceFile ->
                    val relative = sourceFile.relativeTo(actor).invariantSeparatorsPath
                    val targetFile = ContainedPaths.resolveWithin(target, relative, "Actor dependency")
                    if (targetFile.exists() && relative != "manifest.json") {
                        require(
                            sameBytes(sourceFile, targetFile) ||
                                ActorDependencyMatch.equivalentRevision(relative, sourceFile, targetFile),
                        ) {
                            "Actor dependency conflicts with local revision"
                        }
                    }
                }
                actor.walkTopDown().filter(File::isFile).sortedBy { it.name == "manifest.json" }.forEach { sourceFile ->
                    val targetFile = ContainedPaths.resolveWithin(
                        target,
                        sourceFile.relativeTo(actor).invariantSeparatorsPath,
                        "Actor dependency",
                    )
                    if (!targetFile.exists()) DurableFile.copy(sourceFile, targetFile)
                }
            }
            require(stagingDir.renameTo(finalDir)) { "Could not finish importing this project" }
            manifest.projectId
        } catch (error: Exception) {
            stagingDir.deleteRecursively()
            throw error
        }
    }.fold(::value, ::failure)

    private fun addFile(zip: ZipOutputStream, source: File, path: String) {
        zip.putNextEntry(ZipEntry(path))
        source.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun sameBytes(first: File, second: File): Boolean {
        if (first.length() != second.length()) return false
        return first.inputStream().buffered().use { a ->
            second.inputStream().buffered().use { b ->
                val left = ByteArray(32 * 1024)
                val right = ByteArray(left.size)
                while (true) {
                    val count = a.read(left)
                    if (count < 0) return@use true
                    var offset = 0
                    while (offset < count) {
                        val read = b.read(right, offset, count - offset)
                        if (read < 0) return@use false
                        offset += read
                    }
                    if ((0 until count).any { left[it] != right[it] }) return@use false
                }
                @Suppress("UNREACHABLE_CODE")
                false
            }
        }
    }

    private fun <T> value(value: T): StoreResult<T> = StoreResult.Value(value)
    private fun failure(error: Throwable): StoreResult.Failure =
        StoreResult.Failure(error.message ?: "Project archive operation failed", error)
}
