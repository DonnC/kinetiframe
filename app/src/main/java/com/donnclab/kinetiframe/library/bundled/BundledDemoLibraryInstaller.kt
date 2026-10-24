package com.donnclab.kinetiframe.library.bundled

import com.donnclab.kinetiframe.actors.data.LibraryItemKind
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.data.storage.DurableFile
import com.donnclab.kinetiframe.data.storage.StoreResult
import com.donnclab.kinetiframe.library.data.LibraryRepository
import java.io.File
import java.io.InputStream

/** Separate receipts deliberately migrate existing installs, even when their demo projects were
 * edited/deleted. Library payloads come from the bundled originals, never the user's project.
 * Standalone payloads are streamed with a size bound; no bitmap/audio decoding or full ZIP heap.
 */
class BundledDemoLibraryInstaller(
    private val files: DocumentFileStore,
    private val openAsset: (String) -> InputStream,
    private val library: LibraryRepository = LibraryRepository(files),
) {
    fun install(): BundledDemoInstaller.Result = synchronized(lock) {
        val receipts = File(files.projectsRoot().parentFile, "demo-receipts/library-v1").apply { mkdirs() }
        var installed = 0
        val errors = mutableListOf<String>()
        entries.forEach { entry ->
            val receipt = File(receipts, "${entry.id}.installed")
            if (receipt.isFile) return@forEach
            try {
                // Do not replace even an incomplete/user-owned target. Receipt means an explicitly
                // deleted starter resource will not reappear next launch.
                if (!files.libraryDirectory(entry.id).exists()) {
                    val staging = File.createTempFile("resource-", ".${entry.member.substringAfterLast('.')}", receipts)
                    try {
                        extract(entry, staging)
                        when (
                            val result = library.exportAsset(
                                entry.kind,
                                entry.id,
                                entry.name,
                                staging,
                                tags =
                                (
                                    if (entry.id.startsWith(
                                            "ls-",
                                        )
                                    ) {
                                        setOf("loose-signal", "starter")
                                    } else {
                                        setOf("demo", "starter")
                                    }
                                    ) +
                                    (if (entry.kind == LibraryItemKind.AUDIO) setOf(entry.audioTag) else emptySet()) +
                                    entry.tags,
                                nowEpochMs = 1791158400000L,
                            )
                        ) {
                            is StoreResult.Value -> installed++
                            is StoreResult.Failure -> error(result.message)
                        }
                    } finally {
                        staging.delete()
                    }
                }
                DurableFile.write(receipt) { it.write("installed-library-v1\n".toByteArray()) }
            } catch (error: Exception) {
                errors += "${entry.id}: ${error.message ?: "Could not install library resource"}"
            }
        }
        BundledDemoInstaller.Result(installed, errors)
    }

    private fun extract(entry: Entry, destination: File) {
        openAsset(entry.directAsset).buffered().use { copyBounded(it, destination) }
    }

    private fun copyBounded(input: InputStream, destination: File) {
        var bytes = 0L
        val buffer = ByteArray(16 * 1024)
        destination.outputStream().buffered().use { output ->
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                bytes += count
                require(bytes <= 8 * 1024 * 1024) { "Bundled resource exceeds size limit" }
                output.write(buffer, 0, count)
            }
        }
        require(bytes > 0) { "Empty bundled resource" }
    }

    private data class Entry(
        val id: String,
        val name: String,
        val kind: LibraryItemKind,
        val member: String,
        val directAsset: String,
        val audioTag: String = "sfx",
        val tags: Set<String> = emptySet(),
    )
    companion object {
        private val lock = Any()
        private val entries = listOf(
            Entry(
                "demo-stage-background-v1",
                "Practice stage",
                LibraryItemKind.BACKGROUND_IMAGE,
                member = "stage.png",
                directAsset = "library/v1/stage.png",
            ),
            Entry(
                "demo-ball-audio-v1",
                "Stubborn Ball · original SFX",
                LibraryItemKind.AUDIO,
                member = "stubborn-ball.wav",
                directAsset = "library/v1/stubborn-ball.wav",
            ),
            Entry(
                "demo-victory-audio-v1",
                "Small Victory · original SFX",
                LibraryItemKind.AUDIO,
                member = "small-victory.wav",
                directAsset = "library/v1/small-victory.wav",
            ),
            Entry(
                "demo-spar-audio-v1",
                "Latchline Spar · original SFX",
                LibraryItemKind.AUDIO,
                member = "latchline-spar.wav",
                directAsset = "library/v1/latchline-spar.wav",
            ),
            Entry(
                "demo-plan-dialogue-v1",
                "Run Through the Plan · synthetic dialogue",
                LibraryItemKind.AUDIO,
                member = "run-through-plan.wav",
                directAsset = "library/v1/run-through-plan.wav",
                audioTag = "voice",
            ),
            Entry(
                "ls-room-background-v2",
                "Loose Signal · original room",
                LibraryItemKind.BACKGROUND_IMAGE,
                member = "ls-room-background-v2.png",
                directAsset = "loose-signal/v2/ls-room-background-v2.png",
            ),
        ) + BundledSceneBackgrounds.entries.map { scene ->
            Entry(
                scene.libraryId,
                "Scene · ${scene.name}",
                LibraryItemKind.BACKGROUND_IMAGE,
                member = "${scene.slug}.png",
                directAsset = scene.assetPath,
                tags = setOf("background", "scene", scene.category),
            )
        }
    }
}
