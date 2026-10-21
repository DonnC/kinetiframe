package com.donnclab.kinetiframe.data.demo

import com.donnclab.kinetiframe.data.actor.ActorFileStore
import com.donnclab.kinetiframe.data.actor.ActorStoreResult
import com.donnclab.kinetiframe.data.actor.LibraryItemKind
import com.donnclab.kinetiframe.data.library.LibraryRepository
import com.donnclab.kinetiframe.data.library.PackItemEntry
import com.donnclab.kinetiframe.data.library.PackManifest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CuratedLibraryMigrationTest {
    @TempDir lateinit var root: File

    @Test fun `cleanup is recoverable preserves background and cast and runs once`() {
        val files = ActorFileStore(root)
        val pixel = File(root, "pixel.png").apply { writeBytes(byteArrayOf(1)) }
        BundledDemoInstaller.curatedPackIds.forEach {
            assertTrue(
                LibraryRepository(files).exportPack(
                    it,
                    "Test pack",
                    PackManifest(
                        "Test pack",
                        totalItems = 1,
                        canvasDimension = 1,
                        items = listOf(PackItemEntry("frame_000.png", 0, "Pixel")),
                    ),
                    mapOf("frame_000.png" to pixel),
                    nowEpochMs = 0,
                ) is ActorStoreResult.Value,
            )
        }
        CuratedLibraryMigration.castIds.forEach { files.actorDirectory(it).mkdirs() }
        files.projectDirectory(BundledDemoInstaller.TAVI_PRESENTATION_ID).mkdirs()
        val trial = files.projectDirectory("trial-project").apply { mkdirs() }
        File(trial, "drawing.txt").writeText("user trial pixels")
        files.actorDirectory("trial-actor").mkdirs()
        files.libraryDirectory("old-mouth-pack").mkdirs()
        val image = File(root, "scene.png").apply { writeBytes(byteArrayOf(1)) }
        assertTrue(
            LibraryRepository(files).exportAsset(
                LibraryItemKind.BACKGROUND_IMAGE,
                "keep-scene",
                "Scene",
                image,
                nowEpochMs = 0,
            ) is ActorStoreResult.Value,
        )
        assertEquals(ActorStoreResult.Value(3), CuratedLibraryMigration(files).migrate())
        assertFalse(trial.exists())
        assertEquals(
            "user trial pixels",
            File(root, "archive/curated-v3/projects/trial-project/drawing.txt").readText(),
        )
        assertTrue(files.libraryDirectory("keep-scene").exists())
        assertTrue(files.projectDirectory(BundledDemoInstaller.TAVI_PRESENTATION_ID).exists())
        assertTrue(CuratedLibraryMigration.castIds.all { files.actorDirectory(it).exists() })
        val future = files.projectDirectory("future-project").apply { mkdirs() }
        assertEquals(ActorStoreResult.Value(0), CuratedLibraryMigration(files).migrate())
        assertTrue(future.exists())
    }

    @Test fun `missing replacements do not remove trials`() {
        val files = ActorFileStore(root)
        val trial = files.projectDirectory("trial-project").apply { mkdirs() }
        assertTrue(CuratedLibraryMigration(files).migrate() is ActorStoreResult.Failure)
        assertTrue(trial.exists())
    }
}
