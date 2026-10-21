package com.donnclab.kinetiframe.data.demo

import com.donnclab.kinetiframe.data.actor.ActorFileStore
import com.donnclab.kinetiframe.data.actor.ActorStoreResult
import com.donnclab.kinetiframe.data.actor.LibraryItemKind
import com.donnclab.kinetiframe.data.actor.LibraryScope
import com.donnclab.kinetiframe.data.library.LibraryRepository
import com.donnclab.kinetiframe.ui.common.model.AssetKind
import com.donnclab.kinetiframe.ui.library.LocalLibraryCatalog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BundledDemoLibraryInstallerTest {
    @TempDir lateinit var root: File
    private val assets = File("src/main/assets").takeIf(File::isDirectory) ?: File("app/src/main/assets")
    private fun installer(files: ActorFileStore) =
        BundledDemoLibraryInstaller(files, { File(assets, it).inputStream() })
    private fun <T> value(result: ActorStoreResult<T>): T = when (result) {
        is ActorStoreResult.Value -> result.value
        is ActorStoreResult.Failure -> throw AssertionError(result.message, result.cause)
    }

    @Test fun `migration seeds real independent background and audio with no projects required`() {
        val files = ActorFileStore(root)
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
        val catalog = LocalLibraryCatalog(ActorFileStore(root), LibraryRepository(ActorFileStore(root))).load()
        assertEquals(12, catalog.projects.count { it.details.kind == AssetKind.BACKGROUND_IMAGE })
        assertEquals(4, catalog.audio.size)
        assertEquals(3, catalog.audio.count { it.tagLabel == "SFX" })
        assertEquals(1, catalog.audio.count { it.tagLabel == "VOICE" })
        assertTrue(catalog.actors.isEmpty())
        assertEquals(0, installer(files).install().installed)
    }

    @Test fun `deleted seed stays deleted and existing user content is never replaced`() {
        val files = ActorFileStore(root)
        val existing = files.libraryDirectory("demo-stage-background-v1").apply { mkdirs() }
        val sentinel = File(existing, "mine.txt").apply { writeText("keep") }
        assertEquals(15, installer(files).install().installed)
        assertEquals("keep", sentinel.readText())
        value(LibraryRepository(files).delete("demo-ball-audio-v1"))
        assertEquals(0, installer(ActorFileStore(root)).install().installed)
        assertFalse(files.libraryDirectory("demo-ball-audio-v1").exists())
    }

    @Test fun `failed stream retries independently without changing committed resources`() {
        val files = ActorFileStore(root)
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
        val files = ActorFileStore(root)
        val library = LibraryRepository(files)
        val source = File(root, "drawing.png").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        value(library.exportAsset(LibraryItemKind.FRAME, "shared", "Personal drawing", source, nowEpochMs = 1))
        for (id in listOf("first", "second")) {
            val project = com.donnclab.kinetiframe.engine.model.ProjectDocument(id = id)
            value(
                com.donnclab.kinetiframe.data.project.ProjectRepository(files).save(
                    com.donnclab.kinetiframe.data.project.ProjectManifest(
                        projectId = id,
                        displayName = id,
                        stage = project.stage,
                        fps = project.fps,
                        createdAtEpochMs = 1,
                        updatedAtEpochMs = 1,
                    ),
                    com.donnclab.kinetiframe.data.project.ProjectSaveDocument(
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
        val loaded = LocalLibraryCatalog(files, library).load()
        assertEquals(2, loaded.projects.count { it.details.kind == AssetKind.PROJECT })
        assertEquals(3, loaded.projects.count { it.details.kind == AssetKind.FRAME })
        assertEquals(3, loaded.items.keys.distinct().size)
        assertTrue(loaded.audio.isEmpty() && loaded.packs.isEmpty() && loaded.actors.isEmpty())
    }
}
