package com.donnclab.kinetiframe.library.domain

import android.graphics.Bitmap
import android.graphics.Color
import com.donnclab.kinetiframe.actors.data.ActorBundleRepository
import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.core.actor.document.LibraryScope
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.library.model.PackItemEntry
import com.donnclab.kinetiframe.library.model.PackManifest
import org.junit.Assert.assertEquals
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
class LibraryTransfersTest {
    @get:Rule val folder = TemporaryFolder()

    private val files by lazy { DocumentFileStore(folder.newFolder("store")) }
    private val library by lazy { LibraryRepository(files) }
    private val transfers by lazy {
        val bundles = ActorBundleRepository(files, ActorRepository(files), PerformanceRepository(files))
        LibraryTransfers(library, bundles, folder.newFolder("cache")) { 7L }
    }

    private fun bitmap(width: Int, height: Int) =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }

    private fun png(name: String, width: Int, height: Int): File = File(folder.root, name).apply {
        outputStream().use { bitmap(width, height).compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun <T> value(result: StoreResult<T>): T = (result as StoreResult.Value).value

    @Test fun `picked backgrounds and frames arrive decoded`() {
        val room = value(
            library.exportAsset(
                LibraryItemKind.BACKGROUND_IMAGE,
                "room",
                "Room",
                png("room.png", 40, 30),
                nowEpochMs = 1L,
            ),
        )
        val wave =
            value(library.exportAsset(LibraryItemKind.FRAME, "wave", "Wave", png("wave.png", 20, 10), nowEpochMs = 1L))
        val audio = folder.newFolder("audio")

        val background = transfers.pick(room, "project-a", 5L, audio).getOrThrow() as LibraryPick.Background
        val stamp = transfers.pick(wave, "project-a", 5L, audio).getOrThrow() as LibraryPick.Stamp

        assertEquals(40, background.bitmap.width)
        assertEquals(20, stamp.bitmap.width)
    }

    @Test fun `a pack opens as a tray of decoded drawings`() {
        val pack = PackManifest(
            packName = "Hands",
            totalItems = 2,
            canvasDimension = 16,
            items = listOf(
                PackItemEntry(file = "frame_001.png", index = 0, name = "Open"),
                PackItemEntry(file = "frame_002.png", index = 1, name = "Fist"),
            ),
        )
        val manifest = value(
            library.exportPack(
                itemId = "pack-a",
                displayName = "Hands",
                packManifest = pack,
                frameFiles = mapOf("frame_001.png" to png("a.png", 16, 16), "frame_002.png" to png("b.png", 16, 16)),
                nowEpochMs = 1L,
            ),
        )

        val tray = transfers.loadPackTray(manifest, "project-a").getOrThrow()

        assertEquals(listOf("Open", "Fist"), tray.map { it.name })
        assertTrue(tray.all { it.thumbnail.width == 16 })
    }

    @Test fun `a drawn frame is added to the library and a missing background is reported`() {
        val added = value(transfers.exportFrame(bitmap(12, 12), "Frame 3", LibraryScope.PERSONAL, "project-a"))

        assertEquals("Frame 3", added.displayName)
        assertEquals(LibraryItemKind.FRAME, added.kind)
        val missing = transfers.exportBackground(ProjectDocument(id = "project-a"), LibraryScope.PERSONAL)
        assertEquals("No background image is set", (missing as StoreResult.Failure).message)
    }
}
