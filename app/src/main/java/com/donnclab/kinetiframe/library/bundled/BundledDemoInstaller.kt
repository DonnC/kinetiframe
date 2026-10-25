package com.donnclab.kinetiframe.library.bundled

import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.data.storage.DurableFile
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.library.data.PackArchiver
import com.donnclab.kinetiframe.project.data.KframeArchiver
import java.io.File
import java.io.InputStream

/** Prebuilt archives only: no generation, bitmap decoding or network work on the device.
 *
 * Each successful installation gets a durable receipt. A deleted demo stays deleted; an existing
 * directory is never overwritten, even if its document is damaged. Independent receipts allow
 * interrupted first-run installs to resume without replacing the projects already edited.
 * Call on Dispatchers.IO; the injected stream opener keeps storage policy JVM-testable.
 */
class BundledDemoInstaller(
    private val files: DocumentFileStore,
    private val openAsset: (String) -> InputStream,
    private val library: LibraryRepository = LibraryRepository(files),
) {
    data class Result(val installed: Int, val errors: List<String>)

    fun install(): Result = synchronized(installLock) {
        val receipts = File(files.projectsRoot().parentFile, "demo-receipts/v1").apply { mkdirs() }
        var installed = 0
        val errors = mutableListOf<String>()
        entries.forEach { demo ->
            val receipt = File(receipts, "${demo.id}.installed")
            if (receipt.isFile) return@forEach
            val target = if (demo.pack) files.libraryDirectory(demo.id) else files.projectDirectory(demo.id)
            try {
                if (!target.exists()) {
                    // Stream onto disk rather than hold the complete archive in the low-RAM heap.
                    val temporary = File.createTempFile("demo-", ".zip", receipts)
                    try {
                        openAsset("${demo.folder}/${demo.id}.${if (demo.pack) "kpack" else "kframe"}").use { input ->
                            temporary.outputStream().buffered().use { output -> input.copyTo(output) }
                        }
                        val result = if (demo.pack) {
                            PackArchiver.import(
                                files,
                                library,
                                temporary,
                                demo.id,
                                tags = if (demo.id in curatedPackIds) {
                                    setOf("loose-signal", "starter") +
                                        (
                                            if (demo.id.contains("cutouts")) {
                                                setOf("characters", demo.id.split('-')[1])
                                            } else if (demo.id.contains("studio")) {
                                                setOf("props", "studio")
                                            } else {
                                                setOf("expressions")
                                            }
                                            )
                                } else if (demo.id.startsWith("ls-")) {
                                    setOf("loose-signal", "starter", "tavi")
                                } else {
                                    setOf("demo", "starter")
                                },
                                nowEpochMs = CREATED_AT,
                            )
                        } else {
                            KframeArchiver.import(files, temporary)
                        }
                        when (result) {
                            is StoreResult.Value -> {
                                if (!demo.pack) check(result.value == demo.id) { "Unexpected demo project identity" }
                                installed++
                            }

                            is StoreResult.Failure -> error(result.message)
                        }
                    } finally {
                        temporary.delete()
                    }
                }
                DurableFile.write(receipt) { it.write("installed-v1\n".toByteArray()) }
            } catch (error: Exception) {
                errors += "${demo.id}: ${error.message ?: "Could not install demo"}"
            }
        }
        errors += BundledPackKitUpgrade(files, openAsset).upgrade()
        Result(installed, errors)
    }

    private data class Entry(val id: String, val pack: Boolean = false, val folder: String = "demos/v1")

    companion object {
        private val installLock = Any()
        private const val CREATED_AT = 1791158400000L
        const val TAVI_PRESENTATION_ID = "ls-tavi-presentation-v2"
        val projectIds = listOf(TAVI_PRESENTATION_ID)
        val curatedPackIds = listOf(
            "ls-tavi-cutouts-v6",
            "ls-mira-cutouts-v6",
            "ls-kuda-cutouts-v6",
            "ls-expressions-v3",
            "ls-eyes-v3",
            "ls-mouths-v3",
            "ls-studio-items-v3",
        )
        val packIds = curatedPackIds
        fun projectAssetPath(id: String): String {
            require(id == TAVI_PRESENTATION_ID) { "Unknown bundled project" }
            return "loose-signal/v2/$id.kframe"
        }
        private val entries = projectIds.map { Entry(it, folder = projectAssetPath(it).substringBeforeLast('/')) } +
            curatedPackIds.map {
                Entry(
                    it,
                    pack = true,
                    folder = if (it.contains("cutouts-v6")) "loose-signal/curated/v4" else "loose-signal/curated/v3",
                )
            }
    }
}
