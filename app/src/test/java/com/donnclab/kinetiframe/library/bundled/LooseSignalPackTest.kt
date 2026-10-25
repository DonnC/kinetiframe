package com.donnclab.kinetiframe.library.bundled

import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.library.data.LibraryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipFile
import javax.imageio.ImageIO

class LooseSignalPackTest {
    @TempDir lateinit var root: File
    private val assets = File("src/main/assets").takeIf(File::isDirectory) ?: File("app/src/main/assets")
    private val sharedIds = listOf("ls-expressions-v3", "ls-eyes-v3", "ls-mouths-v3", "ls-studio-items-v3")

    private fun packOnlyInstaller(files: DocumentFileStore): BundledDemoInstaller {
        // Existing demo payloads are covered by BundledDemoInstallerTest. Occupied targets
        // model an upgrade, avoiding thousands of unrelated frame extractions in these tests.
        BundledDemoInstaller.projectIds.forEach { files.projectDirectory(it).mkdirs() }
        (BundledDemoInstaller.packIds - sharedIds.toSet()).forEach {
            files.libraryDirectory(it).mkdirs()
        }
        return BundledDemoInstaller(files, {
            com.donnclab.kinetiframe.library.bundled.DemoAssetFixtures.file(assets, it).inputStream()
        })
    }

    @Test fun `shipping packs install reopen and expose real transparent drawings`() {
        val files = DocumentFileStore(root)
        val result = packOnlyInstaller(files).install()
        assertTrue(result.errors.isEmpty(), result.errors.toString())
        assertEquals(4, result.installed)
        val expected = listOf(12, 6, 14, 11)
        var count = 0
        // Recreate the repository to exercise persisted storage, not in-memory authoring data.
        val reopened = LibraryRepository(DocumentFileStore(root))
        sharedIds.zip(expected).forEach { (id, total) ->
            val loaded = reopened.loadPack(id)
            assertTrue(loaded is StoreResult.Value, loaded.toString())
            val (item, manifest) = when (loaded) {
                is StoreResult.Value -> loaded.value
                is StoreResult.Failure -> throw AssertionError(loaded.message, loaded.cause)
            }
            assertTrue(manifest.packName.isNotBlank())
            assertTrue(item.tags.contains("loose-signal"))
            assertEquals(total, manifest.totalItems)
            assertEquals(if (id == "ls-eyes-v3") 256 else 512, manifest.canvasDimension)
            ZipFile(File(assets, "loose-signal/curated/v3/$id.kpack")).use { zip ->
                manifest.items.forEach { drawing ->
                    assertTrue(drawing.name.isNotBlank())
                    val image = zip.getInputStream(zip.getEntry(drawing.file)).use { ImageIO.read(it) }
                    assertNotNull(image)
                    assertTrue(image.width > 0 && image.height > 0)
                    assertTrue(image.colorModel.hasAlpha())
                    val pixels = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
                    assertTrue(pixels.any { it ushr 24 > 0 })
                    assertTrue(pixels.any { it ushr 24 == 0 })
                    count++
                }
            }
        }
        assertEquals(43, count)
    }

    @Test fun `deleted Loose Signal packs stay deleted while untouched packs remain available`() {
        val files = DocumentFileStore(root)
        val installer = packOnlyInstaller(files)
        val result = installer.install()
        assertTrue(result.errors.isEmpty())
        assertEquals(4, result.installed)
        val removed = files.libraryDirectory(sharedIds.first())
        assertTrue(removed.deleteRecursively())
        assertEquals(0, installer.install().installed)
        assertFalse(removed.exists())
        assertTrue(files.libraryDirectory(sharedIds.last()).isDirectory)
    }
}
