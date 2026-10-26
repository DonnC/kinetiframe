package com.donnclab.kinetiframe.library.domain

import android.graphics.Bitmap
import com.donnclab.kinetiframe.actors.data.ActorArtworkDecoder
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.library.model.PackItemEntry
import java.io.File

/**
 * Library drawings read as actor artwork: a frame item, or one drawing from a pack. Each read
 * copies the drawing to a temporary file in [temporaryDirectory], decodes it and deletes the
 * copy. The caller owns the returned bitmap. Call off the main thread.
 */
class LibraryArtwork(private val library: LibraryRepository, private val temporaryDirectory: File) {
    /** Decodes the frame [item]. Throws when it can't be read. */
    fun read(item: LibraryItemManifest): Bitmap = decode(preservePadding = false) { destination ->
        library.copyAssetTo(item.libraryItemId, destination, item.scope, item.sourceProjectId)
    }

    /**
     * Decodes [entry] from [pack]. A drawing with a registration keeps its transparent padding,
     * because the registration's pivot is measured in the full source image. Throws when it can't
     * be read.
     */
    fun readPackItem(pack: LibraryItemManifest, entry: PackItemEntry): Bitmap =
        decode(preservePadding = entry.registration != null) { destination ->
            library.copyPackItemTo(pack.libraryItemId, entry.file, destination, pack.scope, pack.sourceProjectId)
        }

    /** The drawings in [pack], in its own order. */
    fun packItems(pack: LibraryItemManifest): StoreResult<List<PackItemEntry>> =
        when (val loaded = library.loadPack(pack.libraryItemId, pack.scope, pack.sourceProjectId)) {
            is StoreResult.Value -> StoreResult.Value(loaded.value.second.items)
            is StoreResult.Failure -> loaded
        }

    /** The file of [entry] in [pack], read in place for a thumbnail. */
    fun packItemFile(pack: LibraryItemManifest, entry: PackItemEntry): File =
        library.packItemFile(pack.libraryItemId, entry.file, pack.scope, pack.sourceProjectId)

    private fun decode(preservePadding: Boolean, copy: (File) -> StoreResult<*>): Bitmap {
        val target = File.createTempFile("actor-artwork-", ".png", temporaryDirectory)
        try {
            when (val result = copy(target)) {
                is StoreResult.Failure -> throw IllegalStateException(result.message)
                is StoreResult.Value -> return ActorArtworkDecoder.decode(target, preservePadding)
            }
        } finally {
            target.delete()
        }
    }
}
