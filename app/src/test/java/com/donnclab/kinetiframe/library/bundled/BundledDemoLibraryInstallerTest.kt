package com.donnclab.kinetiframe.library.bundled

import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.core.actor.document.LibraryScope
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.library.domain.LibraryCatalog
import com.donnclab.kinetiframe.presentation.common.AssetKind
import com.donnclab.kinetiframe.presentation.library.LocalLibraryCatalog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BundledDemoLibraryInstallerTest {
    @TempDir lateinit var root: File
    private val assets = File("src/main/assets").takeIf(File::isDirectory) ?: File("app/src/main/assets")
    private fun installer(files: DocumentFileStore) =
        BundledDemoLibraryInstaller(files, { File(assets, it).inputStream() })
    private fun <T> value(result: StoreResult<T>): T = when (result) {
        is StoreResult.Value -> result.value
        is StoreResult.Failure -> throw AssertionError(result.message, result.cause)
    }

    @Test fun `migration seeds real independent background and audio with no projects required`() {
        val files = DocumentFileStore(root)
        assertEquals(16, installer(files).install().installed)
        val library = LibraryRepository(files)
        val entries = value(library.list())
        assertEquals(12, entries.count { it.kind == LibraryItemKind.BACKGROUND_IMAGE })
        assertEquals(4, entries.count { it.kind == LibraryItemKind.AUDIO })
        entries.forEach { item ->
            val destination = File(root, "copy-${item.libraryItemId}")
            value(library.copyAssetTo(item.libraryItemId, destination))
            assertTrue(destination.length() > 1000)
        }
        val catalog = DocumentFileStore(root).let { store ->
            LocalLibraryCatalog(
                LibraryCatalog(
                    store,
                    LibraryRepository(store),
                    ProjectRepository(store),
                    ActorRepository(store),
                    PerformanceRepository(store),
                ),
            ).load()
        }
        assertEquals(12, catalog.projects.count { it.details.kind == AssetKind.BACKGROUND_IMAGE })
        assertEquals(4, catalog.audio.size)
        assertEquals(3, catalog.audio.count { it.tagLabel == "SFX" })
        assertEquals(1, catalog.audio.count { it.tagLabel == "VOICE" })
        assertTrue(catalog.actors.isEmpty())
        assertEquals(0, installer(files).install().installed)
    }

    @Test fun `deleted seed stays deleted and existing user content is never replaced`() {
        val files = DocumentFileStore(root)
        val existing = files.libraryDirectory("demo-stage-background-v1").apply { mkdirs() }
        val sentinel = File(existing, "mine.txt").apply { writeText("keep") }
        assertEquals(15, installer(files).install().installed)
        assertEquals("keep", sentinel.readText())
        value(LibraryRepository(files).delete("demo-ball-audio-v1"))
        assertEquals(0, installer(DocumentFileStore(root)).install().installed)
        assertFalse(files.libraryDirectory("demo-ball-audio-v1").exists())
    }

    @Test fun `failed stream retries independently without changing committed resources`() {
        val files = DocumentFileStore(root)
        val failed = BundledDemoLibraryInstaller(files, { path ->
            if (path.contains("small-victory")) error("unavailable")
            File(assets, path).inputStream()
        }).install()
        assertEquals(15, failed.installed)
        assertEquals(1, failed.errors.size)
        val original = value(LibraryRepository(files).list()).associate { it.libraryItemId to it.createdAtEpochMs }
        assertEquals(1, installer(files).install().installed)
        value(LibraryRepository(files).list()).filter { it.libraryItemId in original }.forEach {
            assertEquals(original[it.libraryItemId], it.createdAtEpochMs)
        }
    }

    @Test fun `catalog includes project vaults without hiding equal scoped IDs or showing phantom entries`() {
        val files = DocumentFileStore(root)
        val library = LibraryRepository(files)
        val source = File(root, "drawing.png").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        value(library.exportAsset(LibraryItemKind.FRAME, "shared", "Personal drawing", source, nowEpochMs = 1))
        for (id in listOf("first", "second")) {
            val project = com.donnclab.kinetiframe.core.document.ProjectDocument(id = id)
            value(
                com.donnclab.kinetiframe.data.project.ProjectRepository(files).save(
                    com.donnclab.kinetiframe.core.document.ProjectManifest(
                        projectId = id,
                        displayName = id,
                        stage = project.stage,
                        fps = project.fps,
                        createdAtEpochMs = 1,
                        updatedAtEpochMs = 1,
                    ),
                    com.donnclab.kinetiframe.core.document.ProjectSaveDocument(
                        project = project,
                        frameLayers = emptyMap(),
                        totalFrames = 1,
                    ),
                ),
            )
            value(
                library.exportAsset(
                    LibraryItemKind.FRAME,
                    "shared",
                    "Project drawing",
                    source,
                    LibraryScope.PROJECT,
                    id,
                    nowEpochMs = 1,
                ),
            )
        }
        val loaded = LocalLibraryCatalog(
            LibraryCatalog(
                files,
                library,
                ProjectRepository(files),
                ActorRepository(files),
                PerformanceRepository(files),
            ),
        ).load()
        assertEquals(2, loaded.projects.count { it.details.kind == AssetKind.PROJECT })
        assertEquals(3, loaded.projects.count { it.details.kind == AssetKind.FRAME })
        assertEquals(3, loaded.items.keys.distinct().size)
        assertTrue(loaded.audio.isEmpty() && loaded.packs.isEmpty() && loaded.actors.isEmpty())
    }
}
