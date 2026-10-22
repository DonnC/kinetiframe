package com.donnclab.kinetiframe.library.data

import com.donnclab.kinetiframe.actors.data.ActorFileStore
import com.donnclab.kinetiframe.actors.data.ActorStoreResult
import com.donnclab.kinetiframe.actors.data.LibraryItemKind
import com.donnclab.kinetiframe.actors.data.LibraryScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class LibraryRepositoryTest {
    @TempDir lateinit var root: File

    @TempDir lateinit var sourceDir: File

    @Test fun `audio preview reads owned bytes without exporting a copy`() {
        val repo = LibraryRepository(ActorFileStore(root))
        val source = File(sourceDir, "sound.wav").apply { writeText("audio bytes") }
        val manifest =
            requireSuccess(repo.exportAsset(LibraryItemKind.AUDIO, "preview", "Sound", source, nowEpochMs = 1))
        source.delete()
        val before = root.walkTopDown().filter(File::isFile).map { it.relativeTo(root).path }.toList()
        val preview = requireSuccess(repo.assetFile(manifest))
        assertEquals("audio bytes", preview.readText())
        assertEquals(before, root.walkTopDown().filter(File::isFile).map { it.relativeTo(root).path }.toList())
        preview.delete()
        assertTrue(repo.assetFile(manifest) is ActorStoreResult.Failure)
    }

    private fun <T> requireSuccess(result: ActorStoreResult<T>): T = when (result) {
        is ActorStoreResult.Value -> result.value

        is ActorStoreResult.Failure -> throw AssertionError(
            "Expected success but got failure: ${result.message}",
            result.cause,
        )
    }

    @Test fun `editing a pack switches complete payload versions while preserving identity`() {
        val files = ActorFileStore(root)
        val pack = PackManifest(
            packName = "Hands",
            totalItems = 1,
            canvasDimension = 128,
            items = listOf(PackItemEntry(file = "frame_001.png", index = 0, name = "Fist")),
        )
        val image = File(sourceDir, "source.png").apply { writeText("old drawing") }
        val repo = LibraryRepository(files)
        requireSuccess(repo.exportPack("hands", "Hands", pack, mapOf("frame_001.png" to image), nowEpochMs = 1))
        image.writeText("new drawing")
        requireSuccess(repo.exportPack("hands", "Hands revised", pack, mapOf("frame_001.png" to image), nowEpochMs = 1))
        val reopened = LibraryRepository(ActorFileStore(root))
        assertEquals("Hands revised", requireSuccess(reopened.loadPack("hands")).first.displayName)
        assertEquals("new drawing", reopened.packItemFile("hands", "frame_001.png").readText())
        image.delete()
        assertTrue(
            reopened.exportPack(
                "hands",
                "Incomplete",
                pack,
                mapOf("frame_001.png" to image),
                nowEpochMs = 2,
            ) is ActorStoreResult.Failure,
        )
        assertEquals("Hands revised", requireSuccess(reopened.loadPack("hands")).first.displayName)
    }

    @Test fun `a populated library index cannot hide another saved resource`() {
        val rows = mutableMapOf<String, com.donnclab.kinetiframe.actors.data.db.LibraryItemEntity>()
        val dao = object : com.donnclab.kinetiframe.actors.data.db.LibraryItemDao {
            override fun list(scope: String, projectId: String?) = rows.values.toList()
            override fun upsert(entity: com.donnclab.kinetiframe.actors.data.db.LibraryItemEntity) {
                rows[entity.libraryItemId] =
                    entity
            }
            override fun deleteById(libraryItemId: String) {
                rows.remove(libraryItemId)
            }
        }
        val source = File(sourceDir, "sound.m4a").apply { writeText("audio") }
        val files = ActorFileStore(root)
        requireSuccess(
            LibraryRepository(
                files,
                dao = dao,
            ).exportAsset(LibraryItemKind.AUDIO, "one", "One", source, nowEpochMs = 1),
        )
        requireSuccess(
            LibraryRepository(files).exportAsset(LibraryItemKind.AUDIO, "two", "Two", source, nowEpochMs = 2),
        )
        assertEquals(1, rows.size)
        assertEquals(2, requireSuccess(LibraryRepository(ActorFileStore(root), dao = dao).list()).size)
        assertEquals(2, rows.size)
    }

    @Test fun `exporting a raw asset copies it in, and it round trips through list and copyAssetTo`() {
        val repository = LibraryRepository(ActorFileStore(root))
        val source = File(sourceDir, "clip.m4a").apply { writeText("audio-bytes") }

        val manifest = requireSuccess(
            repository.exportAsset(
                LibraryItemKind.AUDIO,
                "lib-a",
                "Punchy Reel Sting",
                source,
                tags = setOf("trending", "sfx"),
                nowEpochMs = 1L,
            ),
        )
        assertEquals(LibraryItemKind.AUDIO, manifest.kind)
        assertEquals(setOf("trending", "sfx"), manifest.tags)

        val listed = requireSuccess(repository.list())
        assertEquals(1, listed.size)
        assertEquals("Punchy Reel Sting", listed.single().displayName)

        val destination = File(sourceDir, "imported.m4a")
        val (copiedManifest, copiedFile) = requireSuccess(repository.copyAssetTo("lib-a", destination))
        assertEquals("Punchy Reel Sting", copiedManifest.displayName)
        assertEquals("audio-bytes", copiedFile.readText())

        // The library keeps its own copy -- deleting the original source doesn't affect it.
        source.delete()
        val (_, copiedAgain) = requireSuccess(repository.copyAssetTo("lib-a", File(sourceDir, "imported2.m4a")))
        assertEquals("audio-bytes", copiedAgain.readText())
    }

    @Test fun `exporting a non-raw-asset kind is refused`() {
        val repository = LibraryRepository(ActorFileStore(root))
        val source = File(sourceDir, "x.json").apply { writeText("{}") }
        val result = repository.exportAsset(LibraryItemKind.ACTOR, "lib-a", "Nope", source, nowEpochMs = 1L)
        assertTrue(result is ActorStoreResult.Failure)
    }

    @Test fun `deleting a library item removes it from the listing`() {
        val repository = LibraryRepository(ActorFileStore(root))
        val source = File(sourceDir, "bg.png").apply { writeText("png-bytes") }
        requireSuccess(
            repository.exportAsset(LibraryItemKind.BACKGROUND_IMAGE, "lib-bg", "Sunset", source, nowEpochMs = 1L),
        )
        requireSuccess(repository.delete("lib-bg"))
        assertTrue(requireSuccess(repository.list()).isEmpty())
    }

    @Test fun `listing tolerates a mix of library items written by different exporters`() {
        // Simulates ActorBundleRepository and LibraryRepository both writing into the same
        // library/ root -- list() must surface both regardless of which one wrote a given item.
        val files = ActorFileStore(root)
        val repository = LibraryRepository(files)
        val source = File(sourceDir, "sting.mp3").apply { writeText("bytes") }
        requireSuccess(repository.exportAsset(LibraryItemKind.AUDIO, "lib-audio", "Sting", source, nowEpochMs = 1L))

        val json = kotlinx.serialization.json.Json { prettyPrint = true }
        val actorManifest = com.donnclab.kinetiframe.actors.data.LibraryItemManifest(
            libraryItemId = "lib-actor",
            kind = LibraryItemKind.ACTOR,
            displayName = "Alice",
            sourceActorId = "actor-alice",
            createdAtEpochMs = 2L,
        )
        files.writeAtomically(
            File(files.libraryDirectory("lib-actor"), "manifest.json"),
            json.encodeToString(com.donnclab.kinetiframe.actors.data.LibraryItemManifest.serializer(), actorManifest),
        )

        val listed = requireSuccess(repository.list())
        assertEquals(setOf("Sting", "Alice"), listed.map { it.displayName }.toSet())
    }

    @Test fun `PROJECT-scoped items are isolated to their own project vault, PERSONAL items are shared`() {
        val repository = LibraryRepository(ActorFileStore(root))
        val source = File(sourceDir, "clip.wav").apply { writeText("bytes") }

        requireSuccess(
            repository.exportAsset(
                LibraryItemKind.AUDIO,
                "lib-personal",
                "Global Sting",
                source,
                nowEpochMs = 1L,
            ),
        )
        requireSuccess(
            repository.exportAsset(
                LibraryItemKind.AUDIO,
                "lib-project",
                "Episode-Only Sting",
                source,
                scope = LibraryScope.PROJECT,
                projectId = "project-1",
                nowEpochMs = 2L,
            ),
        )

        assertEquals(listOf("Global Sting"), requireSuccess(repository.list()).map { it.displayName })
        assertEquals(
            listOf("Episode-Only Sting"),
            requireSuccess(repository.list(LibraryScope.PROJECT, "project-1")).map { it.displayName },
        )
        // A different project's vault never sees another project's items.
        assertTrue(requireSuccess(repository.list(LibraryScope.PROJECT, "project-2")).isEmpty())
    }

    @Test fun `exporting a four-item PACK writes every frame plus meta json and round trips every item`() {
        val repository = LibraryRepository(ActorFileStore(root))
        val frameA = File(sourceDir, "a.png").apply { writeText("pixels-a") }
        val frameB = File(sourceDir, "b.png").apply { writeText("pixels-b") }
        val frameC = File(sourceDir, "c.png").apply { writeText("pixels-c") }
        val frameD = File(sourceDir, "d.png").apply { writeText("pixels-d") }
        val pack = PackManifest(
            packName = "Mouth Pack",
            author = "Animator",
            totalItems = 4,
            canvasDimension = 512,
            items = listOf(
                PackItemEntry(file = "frame_001.png", index = 0, name = "Mouth_Open", desc = "loud"),
                PackItemEntry(file = "frame_002.png", index = 1, name = "Mouth_Closed"),
                PackItemEntry(file = "frame_003.png", index = 2, name = "Mouth_Smile"),
                PackItemEntry(file = "frame_004.png", index = 3, name = "Mouth_Frown"),
            ),
        )

        val manifest = requireSuccess(
            repository.exportPack(
                "lib-pack",
                "Mouth Pack",
                pack,
                mapOf(
                    "frame_001.png" to frameA,
                    "frame_002.png" to frameB,
                    "frame_003.png" to frameC,
                    "frame_004.png" to frameD,
                ),
                nowEpochMs = 1L,
            ),
        )
        assertEquals(LibraryItemKind.PACK, manifest.kind)

        val (loadedManifest, loadedPack) = requireSuccess(repository.loadPack("lib-pack"))
        assertEquals("Mouth Pack", loadedManifest.displayName)
        assertEquals(4, loadedPack.totalItems)
        assertEquals(
            listOf("Mouth_Open", "Mouth_Closed", "Mouth_Smile", "Mouth_Frown"),
            loadedPack.items.map {
                it.name
            },
        )

        loadedPack.items.forEachIndexed { index, entry ->
            val destination = File(sourceDir, "stamped-${index + 1}.png")
            val copied = requireSuccess(repository.copyPackItemTo("lib-pack", entry.file, destination))
            assertEquals(listOf("pixels-a", "pixels-b", "pixels-c", "pixels-d")[index], copied.readText())
        }
    }

    @Test fun `exporting a PACK with frame files that don't match its own item list is refused`() {
        val repository = LibraryRepository(ActorFileStore(root))
        val frameA = File(sourceDir, "a.png").apply { writeText("bytes") }
        val pack = PackManifest(
            packName = "Bad Pack",
            totalItems = 1,
            canvasDimension = 512,
            items = listOf(PackItemEntry(file = "frame_001.png", index = 0, name = "Only")),
        )
        val result = repository.exportPack(
            "lib-bad",
            "Bad Pack",
            pack,
            mapOf("wrong_name.png" to frameA),
            nowEpochMs = 1L,
        )
        assertTrue(result is ActorStoreResult.Failure)
    }
}
