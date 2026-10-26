package com.donnclab.kinetiframe.library.domain

import android.graphics.Bitmap
import android.graphics.Color
import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.core.actor.document.LibraryScope
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.library.model.PackFrameTag
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PackDraftsTest {
    @get:Rule val folder = TemporaryFolder()

    private val files by lazy { DocumentFileStore(folder.newFolder("store")) }
    private val library by lazy { LibraryRepository(files) }
    private val drafts by lazy {
        PackDrafts(files, ProjectRepository(files), library, folder.newFolder("cache")) { 7L }
    }

    private fun <T> value(result: StoreResult<T>): T = (result as StoreResult.Value).value

    private suspend fun draw(draftId: String, colors: List<Int>) = coroutineScope {
        val frames = FrameCacheManager(File(files.projectDirectory(draftId), "frames"), this)
        colors.forEachIndexed { index, color ->
            frames.putFrame(
                "main",
                index,
                Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(color)
                },
            )
        }
        frames.persistAllToDisk(listOf("main"), colors.size)
    }

    @Test fun `a new pack is saved from its tagged frames and its draft is deleted`() = runBlocking {
        val draftId = value(drafts.create(8))
        draw(draftId, listOf(Color.RED, Color.BLUE))
        val tags = mapOf(0 to PackFrameTag("Happy"), 1 to PackFrameTag("Sad", "Eyes down"))

        value(drafts.save(draftId, 8, "Moods", 2, tags, editing = null))

        assertFalse(files.projectDirectory(draftId).exists())
        val pack = value(library.list(LibraryScope.PERSONAL)).single { it.kind == LibraryItemKind.PACK }
        assertEquals("Moods", pack.displayName)
        val (_, manifest) = value(library.loadPack(pack.libraryItemId))
        assertEquals(listOf("Happy", "Sad"), manifest.items.map { it.name })
        assertEquals("Eyes down", manifest.items[1].desc)
    }

    @Test fun `a saved pack reopens with its drawings and tags and saving keeps its identity`() = runBlocking {
        val first = value(drafts.create(8))
        draw(first, listOf(Color.RED, Color.BLUE))
        value(drafts.save(first, 8, "Moods", 2, mapOf(0 to PackFrameTag("Happy"), 1 to PackFrameTag("Sad")), null))
        val saved = value(library.list(LibraryScope.PERSONAL)).single { it.kind == LibraryItemKind.PACK }

        val draft = value(drafts.openForEdit(saved.libraryItemId, LibraryScope.PERSONAL, null))
        assertEquals(mapOf(0 to PackFrameTag("Happy"), 1 to PackFrameTag("Sad")), draft.tags)
        assertEquals(8, draft.canvasDimension)
        val blue = coroutineScope {
            FrameCacheManager(File(files.projectDirectory(draft.draftId), "frames"), this).getFrame("main", 1)
        }
        assertEquals(Color.BLUE, blue!!.getPixel(4, 4))

        value(drafts.save(draft.draftId, 8, "Moods", 1, mapOf(0 to PackFrameTag("Glad")), draft.pack))
        val packs = value(library.list(LibraryScope.PERSONAL)).filter { it.kind == LibraryItemKind.PACK }
        assertEquals(listOf(saved.libraryItemId), packs.map { it.libraryItemId })
        assertEquals(listOf("Glad"), value(library.loadPack(saved.libraryItemId)).second.items.map { it.name })
    }

    @Test fun `an untagged frame stops the save and keeps the draft`() = runBlocking {
        val draftId = value(drafts.create(8))
        draw(draftId, listOf(Color.RED, Color.BLUE))

        val result = drafts.save(draftId, 8, "Moods", 2, mapOf(0 to PackFrameTag("Happy")), null)

        assertTrue((result as StoreResult.Failure).message.contains("frame 2 is untagged"))
        assertTrue(files.projectDirectory(draftId).exists())
    }

    @Test fun `discarding deletes the draft`() {
        val draftId = value(drafts.create(8))

        drafts.discard(draftId)

        assertFalse(files.projectDirectory(draftId).exists())
    }
}
