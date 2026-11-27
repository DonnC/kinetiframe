package com.donnclab.kinetiframe.library.bundled

import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.library.data.LibraryRepository
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BundledSceneBackgroundsTest {
    @TempDir lateinit var root: File
    private val assets = File("src/main/assets").takeIf(File::isDirectory) ?: File("app/src/main/assets")
    private fun <T> value(result: StoreResult<T>): T = when (result) {
        is StoreResult.Value -> result.value
        is StoreResult.Failure -> throw AssertionError(result.message, result.cause)
    }
    private fun install(files: DocumentFileStore) =
        BundledDemoLibraryInstaller(files, { File(assets, it).inputStream() }).install()

    @Test fun `fourteen unique real landscape WebPs cover all five categories within streaming budget`() {
        val scenes = BundledSceneBackgrounds.entries
        assertEquals(14, scenes.size)
        assertEquals(14, scenes.map { it.libraryId }.distinct().size)
        assertEquals(
            setOf("indoor", "outdoor", "education", "performance", "generic"),
            scenes.map {
                it.category
            }.toSet(),
        )
        for (scene in scenes) {
            val file = File(assets, scene.assetPath)
            assertTrue(file.isFile, scene.assetPath)
            // Lossy WebP keeps a painted plate small: well under a megabyte each.
            assertTrue(file.length() in 1..(1024L * 1024), scene.assetPath)
            val header = file.inputStream().use { it.readNBytes(WEBP_HEADER) }
            assertEquals("RIFF", String(header, 0, 4, Charsets.US_ASCII), "RIFF signature")
            assertEquals("WEBPVP8 ", String(header, 8, 8, Charsets.US_ASCII), "lossy WebP")
            fun dimension(at: Int) = ((header[at].toInt() and 0xff) or ((header[at + 1].toInt() and 0xff) shl 8)) and
                0x3fff
            val width = dimension(26)
            val height = dimension(28)
            assertTrue(width >= 1280 && height >= 720 && width > height, "${scene.slug}: ${width}x$height")
        }
    }

    @Test fun `old install gains scenes once and deleting scene does not resurrect it`() {
        val files = DocumentFileStore(root)
        val first = BundledDemoLibraryInstaller(files, { path ->
            if (path.startsWith("backgrounds/")) error("Not included in old build")
            File(assets, path).inputStream()
        }).install()
        assertEquals(6, first.installed)
        assertEquals(14, first.errors.size)
        assertEquals(14, install(files).installed)
        val reopened = LibraryRepository(DocumentFileStore(root))
        val scene = BundledSceneBackgrounds.entries.first()
        val item = value(reopened.list()).single { it.libraryItemId == scene.libraryId }
        assertEquals(LibraryItemKind.BACKGROUND_IMAGE, item.kind)
        assertTrue(item.tags.containsAll(setOf("starter", "scene", scene.category)))
        val saved = value(reopened.assetFile(item))
        assertArrayEquals(File(assets, scene.assetPath).readBytes(), saved.readBytes())
        value(reopened.delete(item.libraryItemId))
        assertEquals(0, install(DocumentFileStore(root)).installed)
        assertFalse(files.libraryDirectory(item.libraryItemId).exists())
    }

    @Test fun `user owned scene ID remains untouched while other scenes install`() {
        val files = DocumentFileStore(root)
        val scene = BundledSceneBackgrounds.entries.first()
        val sentinel = File(
            files.libraryDirectory(scene.libraryId).apply {
                mkdirs()
            },
            "mine.txt",
        ).apply { writeText("my artwork") }
        assertEquals(BundledSceneBackgrounds.entries.size + 5, install(files).installed)
        assertEquals("my artwork", sentinel.readText())
        assertEquals(0, install(DocumentFileStore(root)).installed)
    }

    private companion object {
        /** RIFF header, VP8 chunk header and frame tag up to the width and height fields. */
        const val WEBP_HEADER = 30
    }
}
