package com.donnclab.kinetiframe.library.bundled

import com.donnclab.kinetiframe.actors.data.validate
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.data.storage.StoreResult
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.library.data.PackManifest
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/** Archive only byte-identical shipped v5 kit payloads once the replacement is installed.
 * User-edited packs are not guessed at or deleted. No projects or actor revisions are touched. */
internal class BundledPackKitUpgrade(
    private val files: DocumentFileStore,
    private val openAsset: (String) -> InputStream,
) {
    fun upgrade(): List<String> = listOf("tavi", "mira", "kuda").mapNotNull { character ->
        val oldId = "ls-$character-cutouts-v5"
        val old = files.libraryDirectory(oldId)
        if (!old.isDirectory || !files.libraryDirectory("ls-$character-cutouts-v6").isDirectory) return@mapNotNull null
        if (LibraryRepository(
                files,
            ).loadPack("ls-$character-cutouts-v6") !is StoreResult.Value
        ) {
            return@mapNotNull null
        }
        runCatching {
            val loaded = LibraryRepository(files).loadPack(oldId)
            val manifest = (loaded as? StoreResult.Value)?.value?.second ?: return@runCatching
            val expected = mutableMapOf<String, ByteArray>()
            var sourceManifest: PackManifest? = null
            ZipInputStream(openAsset("loose-signal/curated/v3/$oldId.kpack")).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name ==
                        "meta.json"
                    ) {
                        sourceManifest =
                            Json.decodeFromString<PackManifest>(zip.readBytes().decodeToString()).validate()
                    } else if (entry.name in manifest.items.map { it.file }) {
                        expected[entry.name] = digest(zip)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            if (manifest != sourceManifest || manifest.items.any { item ->
                    val stored = LibraryRepository(files).packItemFile(oldId, item.file)
                    !stored.isFile || !stored.inputStream().use { digest(it) }.contentEquals(expected[item.file])
                }
            ) {
                return@runCatching
            }
            val archiveRoot = File(files.libraryRoot().parentFile, "archive/starter-kits-v4/library").canonicalFile
            val destination = File(archiveRoot, oldId).canonicalFile
            require(
                old.canonicalFile.parentFile == files.libraryRoot().canonicalFile &&
                    destination.parentFile == archiveRoot,
            )
            if (destination.exists()) return@runCatching // An explicitly recovered legacy pack stays recovered.
            files.move(old, destination)
        }.exceptionOrNull()?.let { "Starter pack upgrade: ${it.message}" }
    }
    private fun digest(stream: InputStream): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var count = stream.read(buffer)
        while (count != -1) {
            digest.update(buffer, 0, count)
            count = stream.read(buffer)
        }
        return digest.digest()
    }
}
