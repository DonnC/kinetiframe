package com.donnclab.kinetiframe.library.bundled

import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/** The natural-hands copy ships beside the original Tavi presents and changes only the arms. */
class TaviNaturalHandsTest {
    @TempDir lateinit var root: File
    private val assets = File("src/main/assets").takeIf(File::isDirectory) ?: File("app/src/main/assets")
    private val original = BundledDemoInstaller.TAVI_PRESENTATION_ID
    private val copy = BundledDemoInstaller.TAVI_NATURAL_HANDS_ID

    private fun <T> value(result: StoreResult<T>): T = when (result) {
        is StoreResult.Value -> result.value
        is StoreResult.Failure -> throw AssertionError(result.message, result.cause)
    }

    @Test fun `both samples install and the copy changes only the body and hands layers`() {
        val files = DocumentFileStore(root)
        BundledDemoInstaller.packIds.forEach { files.libraryDirectory(it).mkdirs() }
        val result = BundledDemoInstaller(files, { DemoAssetFixtures.file(assets, it).inputStream() }).install()
        assertTrue(result.errors.isEmpty(), result.errors.toString())
        assertEquals(2, result.installed)

        val projects = ProjectRepository(files)
        val (originalManifest, originalDocument) = value(projects.load(original))
        val (copyManifest, copyDocument) = value(projects.load(copy))
        assertNotEquals(originalManifest.displayName, copyManifest.displayName)
        assertEquals(originalManifest.stage, copyManifest.stage)
        assertEquals(originalManifest.fps, copyManifest.fps)
        assertEquals(originalDocument.totalFrames, copyDocument.totalFrames)
        assertEquals(originalDocument.project.layers.map { it.id }, copyDocument.project.layers.map { it.id })
        assertTrue(copyDocument.project.actorPlacements.isEmpty())

        fun frame(id: String, layer: String, index: Int) =
            File(files.projectDirectory(id), "frames/frame_v2_${layer}_frame_$index.png").readBytes()
        listOf(0, 24, 40, 60, 80, 95).forEach { index ->
            listOf("ls-desk", "ls-eyes", "ls-mouths").forEach { layer ->
                assertArrayEquals(frame(original, layer, index), frame(copy, layer, index), "$layer $index")
            }
        }
        // Forearms moved from the body layer to the hands layer, so the gesture frames differ in both.
        listOf(24, 40, 60, 80).forEach { index ->
            listOf("ls-body", "ls-hands").forEach { layer ->
                assertFalse(frame(original, layer, index).contentEquals(frame(copy, layer, index)), "$layer $index")
            }
        }
    }
}
