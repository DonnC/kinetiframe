package com.donnclab.kinetiframe.library.bundled

import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.data.storage.StoreResult
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.project.data.KframeArchiver
import com.donnclab.kinetiframe.project.data.ProjectRepository
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipFile
import javax.imageio.ImageIO

/** Storage and asset contracts use the real importer, not fixture-only JSON parsing. */
class TaviProductionTest {
    @TempDir lateinit var root: File
    private val assets = File("src/main/assets").takeIf(File::isDirectory) ?: File("app/src/main/assets")
    private fun <T> value(result: StoreResult<T>): T = when (result) {
        is StoreResult.Value -> result.value
        is StoreResult.Failure -> throw AssertionError(result.message, result.cause)
    }
    private fun installer(files: DocumentFileStore, includeProject: Boolean = true): BundledDemoInstaller {
        (BundledDemoInstaller.projectIds - BundledDemoInstaller.TAVI_PRESENTATION_ID).forEach {
            files.projectDirectory(it).mkdirs()
        }
        BundledDemoInstaller.packIds.forEach {
            files.libraryDirectory(it).mkdirs()
        }
        if (!includeProject) files.projectDirectory(BundledDemoInstaller.TAVI_PRESENTATION_ID).mkdirs()
        return BundledDemoInstaller(files, {
            com.donnclab.kinetiframe.library.bundled.DemoAssetFixtures.file(assets, it).inputStream()
        })
    }

    @Test fun `silent editable scene reopens and exports with its exact persistent room plate`() {
        val files = DocumentFileStore(root)
        assertTrue(installer(files).install().errors.isEmpty())
        val id = BundledDemoInstaller.TAVI_PRESENTATION_ID
        val (manifest, saved) = value(ProjectRepository(DocumentFileStore(root)).load(id))
        assertEquals(1280, manifest.stage.width)
        assertEquals(720, manifest.stage.height)
        assertEquals(12, manifest.fps)
        assertEquals(96, saved.totalFrames)
        assertTrue(saved.project.audioClips.isEmpty())
        assertTrue(
            saved.project.actorPlacements.isEmpty(),
            "FxF cutout scene must not advertise a rig it does not contain",
        )
        assertEquals(
            listOf("ls-body", "ls-desk", "ls-hands", "ls-eyes", "ls-mouths"),
            saved.project.layers.map {
                it.id
            },
        )
        assertEquals(4, saved.activeLayerIndex)
        val plate =
            File(
                requireNotNull(
                    saved.project.assets.getValue(requireNotNull(saved.project.backgroundAssetId)).sourcePath,
                ),
            )
        assertArrayEquals(
            com.donnclab.kinetiframe.library.bundled.DemoAssetFixtures.file(
                assets,
                "loose-signal/v2/ls-room-background-v2.png",
            ).readBytes(),
            plate.readBytes(),
        )
        val bodyProofs = mutableSetOf<Int>()
        val mouthProofs = mutableSetOf<Int>()
        listOf(0, 24, 40, 60, 80, 95).forEach { frame ->
            saved.frameLayers.getValue(frame).forEach { layer ->
                assertTrue(frame in layer.populatedFrames)
                val file = File(files.projectDirectory(id), "frames/frame_v2_${layer.id}_frame_$frame.png")
                val image = ImageIO.read(file)
                assertEquals(1280, image.width)
                assertEquals(720, image.height)
                if (layer.id == "ls-body") bodyProofs += file.readBytes().contentHashCode()
                if (layer.id == "ls-mouths") mouthProofs += file.readBytes().contentHashCode()
            }
        }
        assertTrue(bodyProofs.size >= 4)
        assertTrue(mouthProofs.size >= 3)
        val archive = value(KframeArchiver.export(files, id, File(root, "tavi.kframe")))
        val importedFiles = DocumentFileStore(File(root, "reimport"))
        assertEquals(id, value(KframeArchiver.import(importedFiles, archive)))
        val reopened = value(ProjectRepository(importedFiles).load(id)).second
        assertEquals(saved.project.layers.map { it.name }, reopened.project.layers.map { it.name })
        val relocated = File(requireNotNull(reopened.project.assets.getValue("stage").sourcePath))
        assertTrue(
            relocated.canonicalPath.startsWith(importedFiles.projectDirectory(id).canonicalPath + File.separator),
        )
        assertArrayEquals(plate.readBytes(), relocated.readBytes())
    }
}
