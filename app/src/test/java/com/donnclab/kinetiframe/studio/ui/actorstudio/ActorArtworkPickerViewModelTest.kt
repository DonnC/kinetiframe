package com.donnclab.kinetiframe.studio.ui.actorstudio

import android.graphics.Bitmap
import android.graphics.Color
import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.library.domain.LibraryArtwork
import com.donnclab.kinetiframe.library.model.PackItemEntry
import com.donnclab.kinetiframe.library.model.PackManifest
import com.donnclab.kinetiframe.presentation.studio.actorstudio.ActorArtworkPickerEffect
import com.donnclab.kinetiframe.presentation.studio.actorstudio.ActorArtworkPickerIntent
import com.donnclab.kinetiframe.presentation.studio.actorstudio.ActorArtworkPickerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ActorArtworkPickerViewModelTest {
    @get:Rule val folder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private lateinit var library: LibraryRepository
    private lateinit var model: ActorArtworkPickerViewModel

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        library = LibraryRepository(DocumentFileStore(folder.newFolder("store")))
        model = ActorArtworkPickerViewModel(LibraryArtwork(library, folder.newFolder("temp")), dispatcher)
    }

    @After fun teardown() {
        Dispatchers.resetMain()
    }

    private fun png(name: String, width: Int, height: Int): File = File(folder.root, name).apply {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun <T> value(result: StoreResult<T>): T = (result as StoreResult.Value).value

    @Test fun `picking a frame decodes it for the attachment flow`() = runTest(dispatcher) {
        val frame = value(
            library.exportAsset(LibraryItemKind.FRAME, "frame-a", "Wave", png("wave.png", 40, 30), nowEpochMs = 1L),
        )

        model.handleIntent(ActorArtworkPickerIntent.PickItem(frame))
        advanceUntilIdle()

        val selected = model.effect.first() as ActorArtworkPickerEffect.Selected
        assertEquals("Wave", selected.name)
        assertEquals(40, selected.bitmap.width)
        assertFalse(model.state.value.busy)
    }

    @Test fun `a pack lists its drawings and picking one sends it with its name`() = runTest(dispatcher) {
        val pack = PackManifest(
            packName = "Hands",
            totalItems = 1,
            canvasDimension = 64,
            items = listOf(PackItemEntry(file = "frame_001.png", index = 0, name = "Open hand")),
        )
        val manifest = value(
            library.exportPack(
                itemId = "pack-a",
                displayName = "Hands",
                packManifest = pack,
                frameFiles = mapOf("frame_001.png" to png("hand.png", 64, 64)),
                nowEpochMs = 1L,
            ),
        )

        model.handleIntent(ActorArtworkPickerIntent.OpenPack(manifest))
        advanceUntilIdle()
        val entry = model.state.value.items.single()
        assertNotNull(model.packItemFile(entry))

        model.handleIntent(ActorArtworkPickerIntent.PickPackItem(entry))
        advanceUntilIdle()

        assertEquals("Open hand", (model.effect.first() as ActorArtworkPickerEffect.Selected).name)
    }

    @Test fun `an item that can't be read shows an error`() = runTest(dispatcher) {
        val missing = LibraryItemManifest(
            libraryItemId = "missing",
            kind = LibraryItemKind.FRAME,
            displayName = "Gone",
            payloadFileName = "asset.png",
            createdAtEpochMs = 1L,
        )

        model.handleIntent(ActorArtworkPickerIntent.PickItem(missing))
        advanceUntilIdle()

        assertNotNull(model.state.value.error)
        assertFalse(model.state.value.busy)
    }
}
