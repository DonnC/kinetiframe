package com.donnclab.kinetiframe.data.color

import com.donnclab.kinetiframe.data.actor.ActorFileStore
import com.donnclab.kinetiframe.data.actor.ActorStoreResult
import com.donnclab.kinetiframe.data.actor.LibraryItemKind
import com.donnclab.kinetiframe.data.actor.LibraryItemManifest
import com.donnclab.kinetiframe.data.actor.validate
import com.donnclab.kinetiframe.data.library.LibraryRepository
import com.donnclab.kinetiframe.data.storage.DurableFile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Uses the real durable Library, not a second cache or a raster drawing pack. Call on IO. */
class PaletteRepository(
    private val files: ActorFileStore,
    private val library: LibraryRepository = LibraryRepository(files),
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun installStarters() = synchronized(lock) {
        val receipts = File(files.libraryRoot().parentFile, "demo-receipts/palettes-v1").apply { mkdirs() }
        BundledPalettes.entries.forEach { palette ->
            val receipt = File(receipts, palette.id + ".installed")
            if (!receipt.exists()) {
                if (!files.libraryDirectory(palette.id).exists()) {
                    save(palette, setOf("starter", "colours"))
                }
                DurableFile.write(receipt) { it.write("installed\n".toByteArray()) }
            }
        }
    }

    fun save(palette: PaletteDocument, tags: Set<String> = setOf("colours")): LibraryItemManifest {
        palette.validate()
        val staging = File.createTempFile("palette-", ".json", files.libraryRoot().apply { mkdirs() })
        try {
            DurableFile.write(staging) { it.write(json.encodeToString(palette).toByteArray(Charsets.UTF_8)) }
            return value(
                library.exportAsset(
                    LibraryItemKind.PALETTE,
                    palette.id,
                    palette.name,
                    staging,
                    tags = tags,
                    nowEpochMs = System.currentTimeMillis(),
                ),
            )
        } finally {
            staging.delete()
        }
    }

    fun load(item: LibraryItemManifest): PaletteDocument {
        require(item.kind == LibraryItemKind.PALETTE)
        val file = value(library.assetFile(item))
        require(file.length() <= 65_536) { "Palette is too large" }
        return json.decodeFromString<PaletteDocument>(file.readText()).validate().also {
            require(it.id == item.libraryItemId) { "Palette payload identity does not match its manifest" }
        }
    }

    fun list(): List<PaletteDocument> = value(library.list()).filter { it.kind == LibraryItemKind.PALETTE }.map(::load)
    private fun <T> value(result: ActorStoreResult<T>): T = when (result) {
        is ActorStoreResult.Value -> result.value
        is ActorStoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
    }
    companion object {
        private val lock = Any()
    }
}
