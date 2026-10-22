package com.donnclab.kinetiframe.library.bundled

import com.donnclab.kinetiframe.actors.data.ActorBundleRepository
import com.donnclab.kinetiframe.actors.data.ActorFileStore
import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.ActorStoreResult
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.data.storage.DurableFile
import java.io.File
import java.io.InputStream
import java.nio.file.Files

/** Reusable structured samples come from bundled originals, never from edited user projects.
 * Receipt policy preserves explicit deletion and existing Library content. Call on IO.
 */
class BundledActorSampleInstaller(private val files: ActorFileStore, private val openAsset: (String) -> InputStream) {
    fun install(): BundledDemoInstaller.Result = synchronized(lock) {
        val receipts = File(files.projectsRoot().parentFile, "demo-receipts/actor-samples-v2").apply { mkdirs() }
        val priorReceipts = File(files.projectsRoot().parentFile, "demo-receipts/actor-samples-v1")
        val pending = entries.filter { !File(receipts, "${it.itemId}.installed").isFile }
        if (pending.isEmpty()) return@synchronized BundledDemoInstaller.Result(0, emptyList())
        val workspace = Files.createTempDirectory(receipts.toPath(), "originals-").toFile()
        val errors = mutableListOf<String>()
        var installed = 0
        try {
            val originalFiles = ActorFileStore(File(workspace, "source"))
            val archive = File(workspace, "original.zip")
            openAsset(BundledActorSourceReader.ASSET_PATH).use { input ->
                DurableFile.write(archive) { output -> input.copyTo(output) }
            }
            BundledActorSourceReader.read(archive, originalFiles, pending.map { it.sourceId }.toSet())
            val actors = ActorRepository(originalFiles)
            val bundles = ActorBundleRepository(originalFiles, actors, PerformanceRepository(originalFiles))
            val upgrade = BundledStarterKitUpgrade(originalFiles, files)
            pending.forEach { sample ->
                try {
                    val target = files.libraryDirectory(sample.itemId)
                    requireValue(bundles.exportActor(sample.sourceId, sample.itemId, sample.name, CREATED_AT))
                    val previouslyInstalled = File(priorReceipts, "${sample.itemId}.installed").isFile
                    val recovering = if (previouslyInstalled) {
                        upgrade.prepareLibraryReplacement(
                            sample.itemId,
                        )
                    } else {
                        false
                    }
                    if (!target.exists() && (!previouslyInstalled || recovering)) {
                        // Move a complete validated payload once; no partly indexed or visible item.
                        target.parentFile?.mkdirs()
                        Files.move(originalFiles.libraryDirectory(sample.itemId).toPath(), target.toPath())
                        installed++
                    }
                    if (files.actorDirectory(sample.sourceId).exists() ||
                        !previouslyInstalled
                    ) {
                        upgrade.upgradeActor(sample.sourceId)
                    }
                    DurableFile.write(File(receipts, "${sample.itemId}.installed")) {
                        it.write("installed-v2\n".toByteArray())
                    }
                } catch (error: Exception) {
                    errors += "${sample.itemId}: ${error.message}"
                }
            }
        } catch (
            error: Exception,
        ) {
            errors += "Actor samples: ${error.message}"
        } finally {
            workspace.deleteRecursively()
        } // Exact private directory created by this invocation.
        BundledDemoInstaller.Result(installed, errors)
    }

    private fun <T> requireValue(result: ActorStoreResult<T>): T = when (result) {
        is ActorStoreResult.Value -> result.value
        is ActorStoreResult.Failure -> error(result.message)
    }
    private data class Entry(val sourceId: String, val name: String) {
        val itemId = "starter-$sourceId"
    }
    companion object {
        private val lock = Any()
        private const val CREATED_AT = 1791158400000L
        private val entries = listOf("tavi", "mira", "kuda").map { character ->
            val id = "ls-$character-all-views-v3"
            Entry(id, "${character.replaceFirstChar(Char::titlecase)} · All four views")
        }
    }
}
