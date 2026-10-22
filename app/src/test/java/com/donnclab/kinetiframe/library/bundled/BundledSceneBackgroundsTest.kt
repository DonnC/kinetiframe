package com.donnclab.kinetiframe.library.bundled

import com.donnclab.kinetiframe.actors.data.ActorFileStore
import com.donnclab.kinetiframe.actors.data.ActorStoreResult
import com.donnclab.kinetiframe.actors.data.LibraryItemKind
import com.donnclab.kinetiframe.library.data.LibraryRepository
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.DataInputStream
import java.io.File

class BundledSceneBackgroundsTest {
    @TempDir lateinit var root: File
    private val assets = File("src/main/assets").takeIf(File::isDirectory) ?: File("app/src/main/assets")
    private fun <T> value(result: ActorStoreResult<T>): T = when (result) {
        is ActorStoreResult.Value -> result.value
        is ActorStoreResult.Failure -> throw AssertionError(result.message, result.cause)
    }
    private fun install(files: ActorFileStore) =
        BundledDemoLibraryInstaller(files, { File(assets, it).inputStream() }).install()

    @Test fun `ten unique real landscape PNGs cover all five categories within streaming budget`() {
        val scenes = BundledSceneBackgrounds.entries
        assertEquals(10, scenes.size)
        assertEquals(10, scenes.map { it.libraryId }.distinct().size)
        assertEquals(
            setOf("indoor", "outdoor", "education", "performance", "generic"),
            scenes.map {
                it.category
            }.toSet(),
        )
        for (scene in scenes) {
            val file = File(assets, scene.assetPath)
            assertTrue(file.isFile, scene.assetPath)
            assertTrue(file.length() in 1..(8L * 1024 * 1024), scene.assetPath)
            DataInputStream(file.inputStream()).use { input ->
                assertEquals(0x89504e470d0a1a0aUL.toLong(), input.readLong(), "PNG signature")
                assertEquals(13, input.readInt())
                assertEquals(0x49484452, input.readInt(), "IHDR")
                val width = input.readInt()
                val height = input.readInt()
                assertTrue(width >= 1280 && height >= 720 && width > height, "${scene.slug}: ${width}x$height")
            }
        }
    }

    @Test fun `old install gains scenes once and deleting scene does not resurrect it`() {
        val files = ActorFileStore(root)
        val first = BundledDemoLibraryInstaller(files, { path ->
            if (path.startsWith("backgrounds/")) error("Not included in old build")
            File(assets, path).inputStream()
        }).install()
        assertEquals(6, first.installed)
        assertEquals(10, first.errors.size)
        assertEquals(10, install(files).installed)
        val reopened = LibraryRepository(ActorFileStore(root))
        val scene = BundledSceneBackgrounds.entries.first()
        val item = value(reopened.list()).single { it.libraryItemId == scene.libraryId }
        assertEquals(LibraryItemKind.BACKGROUND_IMAGE, item.kind)
        assertTrue(item.tags.containsAll(setOf("starter", "scene", scene.category)))
        val saved = value(reopened.assetFile(item))
        assertArrayEquals(File(assets, scene.assetPath).readBytes(), saved.readBytes())
        value(reopened.delete(item.libraryItemId))
        assertEquals(0, install(ActorFileStore(root)).installed)
        assertFalse(files.libraryDirectory(item.libraryItemId).exists())
    }

    @Test fun `user owned scene ID remains untouched while other scenes install`() {
        val files = ActorFileStore(root)
        val scene = BundledSceneBackgrounds.entries.first()
        val sentinel = File(
            files.libraryDirectory(scene.libraryId).apply {
                mkdirs()
            },
            "mine.txt",
        ).apply { writeText("my artwork") }
        assertEquals(15, install(files).installed)
        assertEquals("my artwork", sentinel.readText())
        assertEquals(0, install(ActorFileStore(root)).installed)
    }
}
