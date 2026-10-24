package com.donnclab.kinetiframe.library.data

import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.data.storage.StoreResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipFile

class PackArchiverTest {
    @TempDir lateinit var root: File

    @TempDir lateinit var work: File

    private fun <T> requireSuccess(result: StoreResult<T>): T = when (result) {
        is StoreResult.Value -> result.value

        is StoreResult.Failure -> throw AssertionError(
            "Expected success but got failure: ${result.message}",
            result.cause,
        )
    }

    @Test fun `export zips exactly the pack's payload files flat, and import round trips it under a new id`() {
        val files = DocumentFileStore(root)
        val library = LibraryRepository(files)
        val frame = File(work, "frame_001.png").apply { writeText("pixels") }
        val pack = PackManifest(
            packName = "Hand Pack",
            totalItems = 1,
            canvasDimension = 512,
            items = listOf(PackItemEntry(file = "frame_001.png", index = 0, name = "Fist")),
        )
        requireSuccess(
            library.exportPack("lib-original", "Hand Pack", pack, mapOf("frame_001.png" to frame), nowEpochMs = 1L),
        )

        val kpackFile = File(work, "hand_pack.kpack")
        requireSuccess(PackArchiver.export(files, library, "lib-original", kpackFile))

        ZipFile(kpackFile).use { zip ->
            val names = zip.entries().asSequence().map { it.name }.toSet()
            assertEquals(setOf("meta.json", "frame_001.png"), names)
        }

        val imported = requireSuccess(PackArchiver.import(files, library, kpackFile, "lib-imported", nowEpochMs = 2L))
        assertEquals(LibraryItemKind.PACK, imported.kind)
        assertEquals("Hand Pack", imported.displayName)

        val (_, importedPack) = requireSuccess(library.loadPack("lib-imported"))
        assertEquals("Fist", importedPack.items.single().name)

        val stamped = File(work, "stamped.png")
        requireSuccess(library.copyPackItemTo("lib-imported", "frame_001.png", stamped))
        assertEquals("pixels", stamped.readText())
    }

    @Test fun `importing a file with no metadata JSON is refused`() {
        val files = DocumentFileStore(root)
        val library = LibraryRepository(files)
        val notAPack = File(work, "not-a-pack.kpack").apply { writeText("just some bytes, not a zip") }
        val result = PackArchiver.import(files, library, notAPack, "lib-x", nowEpochMs = 1L)
        assertTrue(result is StoreResult.Failure)
    }
}
