package com.donnclab.kinetiframe.library.bundled

import com.donnclab.kinetiframe.actors.domain.PerformanceCompatibility
import com.donnclab.kinetiframe.actors.domain.PerformanceEvaluator
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.data.project.ProjectManifest
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.project.ProjectSaveDocument
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.project.data.KframeArchiver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class BundledDemoInstallerTest {
    @TempDir lateinit var root: File
    private val assets = File("src/main/assets").takeIf(File::isDirectory) ?: File("app/src/main/assets")
    private fun installer(files: DocumentFileStore) = BundledDemoInstaller(files, {
        com.donnclab.kinetiframe.library.bundled.DemoAssetFixtures.file(assets, it).inputStream()
    })

    // Policy tests use tiny valid archives. Re-extracting 1,248 production PNGs for every
    // receipt case wastes disk/CI time; the full payload is exercised separately above.
    private fun policyAssets(): File {
        val fixture = File(root, "fixtures").apply { mkdirs() }
        val source = DocumentFileStore(File(root, "fixture-source"))
        BundledDemoInstaller.projectIds.forEach { id ->
            val layer = Layer(name = "Main", id = "main")
            val project = ProjectDocument(id = id, stage = StageDimensions(16, 16), fps = 24, layers = listOf(layer))
            value(
                ProjectRepository(source).save(
                    ProjectManifest(
                        projectId = id,
                        displayName = id,
                        stage = project.stage,
                        fps = 24,
                        createdAtEpochMs = 0,
                        updatedAtEpochMs = 0,
                    ),
                    ProjectSaveDocument(project = project, frameLayers = mapOf(0 to listOf(layer)), totalFrames = 1),
                ),
            )
            value(KframeArchiver.export(source, id, File(fixture, "$id.kframe")))
        }
        BundledDemoInstaller.packIds.forEach { packId ->
            ZipOutputStream(File(fixture, "$packId.kpack").outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("meta.json"))
                zip.write(
                    (
                        """{"pack_name":"Fixture","total_items":1,"canvas_dimension":1,""" +
                            """"items":[{"file":"frame_000.png","index":0,"name":"Dot"}]}"""
                        ).toByteArray(),
                )
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("frame_000.png"))
                zip.write(
                    java.util.Base64.getDecoder().decode(
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a1f8AAAAASUVORK5CYII=",
                    ),
                )
                zip.closeEntry()
            }
        }
        return fixture
    }
    private fun <T> value(result: StoreResult<T>): T = when (result) {
        is StoreResult.Value -> result.value
        is StoreResult.Failure -> throw AssertionError(result.message, result.cause)
    }

    @Test fun `current catalog installs without restoring retired projects`() {
        val files = DocumentFileStore(root)
        val result = installer(files).install()
        assertEquals(emptyList<String>(), result.errors)
        assertEquals(BundledDemoInstaller.projectIds.size + BundledDemoInstaller.packIds.size, result.installed)
        assertEquals(
            BundledDemoInstaller.projectIds.toSet(),
            files.projectsRoot().listFiles().orEmpty().filter(File::isDirectory).map { it.name }.toSet(),
        )
        val (manifest, saved) = value(ProjectRepository(files).load(BundledDemoInstaller.TAVI_PRESENTATION_ID))
        assertEquals(96, saved.totalFrames)
        assertTrue(File(requireNotNull(manifest.thumbnailPath)).isFile)
        saved.project.assets.values.forEach { asset ->
            val path = File(requireNotNull(asset.sourcePath))
            assertTrue(path.isFile)
            assertTrue(
                path.canonicalPath.startsWith(files.projectDirectory(saved.project.id).canonicalPath + File.separator),
            )
        }
    }

    @Test fun `edited existing projects are untouched and deleted installed demos stay deleted`() {
        val files = DocumentFileStore(root)
        val fixture = policyAssets()
        val id = BundledDemoInstaller.projectIds.first()
        val occupied = files.projectDirectory(id).apply { mkdirs() }
        val sentinel = File(occupied, "user-work.txt").apply { writeText("Do not replace") }
        val install = BundledDemoInstaller(files, { File(fixture, File(it).name).inputStream() })
        val result = install.install()
        assertTrue(result.errors.isEmpty(), result.errors.toString())
        assertEquals(BundledDemoInstaller.projectIds.size + BundledDemoInstaller.packIds.size - 1, result.installed)
        assertEquals("Do not replace", sentinel.readText())
        assertFalse(File(occupied, "snapshot.json").exists())
        val deletedId = BundledDemoInstaller.packIds.first()
        files.libraryDirectory(deletedId).deleteRecursively()
        assertEquals(
            0,
            BundledDemoInstaller(DocumentFileStore(root), {
                File(fixture, File(it).name).inputStream()
            }).install().installed,
        )
        assertFalse(files.libraryDirectory(deletedId).exists())
    }

    @Test fun `missing asset can be retried without replacing successfully installed entries`() {
        val files = DocumentFileStore(root)
        val fixture = policyAssets()
        val firstId = BundledDemoInstaller.projectIds.first()
        val broken = BundledDemoInstaller(files, { path ->
            if (path.contains(firstId)) throw java.io.IOException("Simulated interrupted installation")
            File(fixture, File(path).name).inputStream()
        }).install()
        assertEquals(1, broken.errors.size)
        assertEquals(BundledDemoInstaller.projectIds.size + BundledDemoInstaller.packIds.size - 1, broken.installed)
        val survivor = File(files.libraryDirectory(BundledDemoInstaller.packIds.first()), "user-edit.txt").apply {
            writeText("mine")
        }
        val retried = BundledDemoInstaller(files, { File(fixture, File(it).name).inputStream() }).install()
        assertTrue(retried.errors.isEmpty())
        assertEquals(1, retried.installed)
        assertEquals("mine", survivor.readText())
    }

    @Test fun `archives contain no machine specific paths or temporary reference images`() {
        BundledDemoInstaller.projectIds.forEach { id ->
            ZipFile(
                com.donnclab.kinetiframe.library.bundled.DemoAssetFixtures.file(
                    assets,
                    BundledDemoInstaller.projectAssetPath(id),
                ),
            ).use { zip ->
                val names = zip.entries().asSequence().map { it.name }.toList()
                assertFalse(
                    names.any {
                        it.contains("reference", ignoreCase = true) ||
                            it.contains("cache", ignoreCase = true)
                    },
                )
                names.filter { it.endsWith(".json") }.forEach { name ->
                    val text = zip.getInputStream(zip.getEntry(name)).bufferedReader().readText()
                    assertFalse(text.contains("DEVELOPER") || text.contains("C:"), "$id/$name")
                }
            }
        }
    }
}
