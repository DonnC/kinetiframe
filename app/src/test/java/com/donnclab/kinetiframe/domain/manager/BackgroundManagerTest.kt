package com.donnclab.kinetiframe.domain.manager

import android.graphics.Bitmap
import com.donnclab.kinetiframe.engine.asset.ProjectAssetManager
import com.donnclab.kinetiframe.engine.model.AssetKind
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.TimelineState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Verifies the state transitions for the project's single persistent background image -- see
 *  [BackgroundManager]'s doc comment for why this is a plain optional reference plus one cached
 *  bitmap, not a Layer or a timeline placement. Needs Robolectric (like RigAssetManagerTest)
 *  because it exercises real android.graphics.Bitmap creation/recycling, not just state shapes. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackgroundManagerTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var storageDir: File
    private lateinit var assets: ProjectAssetManager
    private var state = TimelineState()
    private val errors = mutableListOf<String>()
    private lateinit var pendingOperations: PendingOperations

    private fun bitmap(w: Int = 4, h: Int = 4): Bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

    private fun manager() = BackgroundManager(
        scope = testScope,
        dispatcher = testDispatcher,
        assets = assets,
        storageDir = storageDir,
        getState = { state },
        updateState = { reducer -> state = reducer(state) },
        onError = errors::add,
        pendingOperations = pendingOperations,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        storageDir = File.createTempFile("kinetiframe_test_bg", "").apply {
            delete()
            mkdir()
        }
        assets = ProjectAssetManager()
        state = TimelineState()
        errors.clear()
        pendingOperations = PendingOperations()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        storageDir.deleteRecursively()
    }

    @Test
    fun `save barrier waits for accepted background import`() {
        manager().handle(TimelineIntent.Background.SetImage(bitmap()))
        var savedAssetId: String? = null
        testScope.launch {
            pendingOperations.awaitIdle()
            savedAssetId = state.project.backgroundAssetId
        }
        assertNull(savedAssetId)
        testScope.advanceUntilIdle()
        assertEquals(requireNotNull(state.project.backgroundAssetId), savedAssetId)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `setting an image registers it as the project's background`() {
        manager().handle(TimelineIntent.Background.SetImage(bitmap(8, 6)))
        testScope.advanceUntilIdle()

        val assetId = requireNotNull(state.project.backgroundAssetId)
        val asset = requireNotNull(state.project.assets[assetId])
        assertEquals(AssetKind.IMPORTED_IMAGE, asset.kind)
        assertEquals(8, asset.width)
        assertEquals(6, asset.height)
        assertTrue("the persisted file should exist on disk", File(requireNotNull(asset.sourcePath)).exists())
        assertTrue(
            "the decoded bitmap should be registered for rendering",
            assets.getAsset(assetId)?.isRecycled == false,
        )
    }

    @Test
    fun `replacing the background releases the previous bitmap and drops its project asset entry`() {
        val mgr = manager()
        mgr.handle(TimelineIntent.Background.SetImage(bitmap()))
        testScope.advanceUntilIdle()
        val firstAssetId = requireNotNull(state.project.backgroundAssetId)
        mgr.handle(TimelineIntent.Background.ToggleVisibility)
        assertFalse(state.canvas.backgroundVisible)

        mgr.handle(TimelineIntent.Background.SetImage(bitmap()))
        testScope.advanceUntilIdle()
        val secondAssetId = requireNotNull(state.project.backgroundAssetId)
        assertTrue("a newly replaced background starts visible", state.canvas.backgroundVisible)

        assertTrue("replacing sets a new asset id", firstAssetId != secondAssetId)
        assertNull("the old asset entry must not linger in project.assets", state.project.assets[firstAssetId])
        assertNull("the old bitmap must be released from the runtime cache", assets.getAsset(firstAssetId))
        assertFalse("the new background must still be usable", assets.getAsset(secondAssetId)?.isRecycled ?: true)
    }

    @Test
    fun `removing the background clears the reference and releases the bitmap`() {
        val mgr = manager()
        mgr.handle(TimelineIntent.Background.SetImage(bitmap()))
        testScope.advanceUntilIdle()
        val assetId = requireNotNull(state.project.backgroundAssetId)

        mgr.handle(TimelineIntent.Background.Remove)
        testScope.advanceUntilIdle()

        assertNull(state.project.backgroundAssetId)
        assertNull(state.project.assets[assetId])
        assertNull(assets.getAsset(assetId))
    }

    @Test
    fun `removing when there is no background is a no-op`() {
        manager().handle(TimelineIntent.Background.Remove)
        testScope.advanceUntilIdle()
        assertNull(state.project.backgroundAssetId)
    }

    @Test fun `saved background restores into a fresh runtime cache with the same identity and pixels`() {
        val image = bitmap(8, 6).apply { eraseColor(android.graphics.Color.MAGENTA) }
        manager().handle(TimelineIntent.Background.SetImage(image))
        testScope.advanceUntilIdle()
        val id = requireNotNull(state.project.backgroundAssetId)
        val files = com.donnclab.kinetiframe.data.actor.ActorFileStore(File(storageDir, "documents"))
        val repository = com.donnclab.kinetiframe.data.project.ProjectRepository(files)
        val project = state.project
        val manifest = com.donnclab.kinetiframe.data.project.ProjectManifest(
            projectId = project.id,
            displayName = "Saved",
            stage = project.stage,
            fps = project.fps,
            createdAtEpochMs = 1,
            updatedAtEpochMs = 1,
        )
        val saved = repository.save(
            manifest,
            com.donnclab.kinetiframe.data.project.ProjectSaveDocument(
                project = project,
                frameLayers = emptyMap(),
                totalFrames = 1,
            ),
        )
        assertTrue(saved is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Value)
        val reopened = repository.load(project.id) as com.donnclab.kinetiframe.data.actor.ActorStoreResult.Value
        state = TimelineState(project = reopened.value.second.project)
        assets = ProjectAssetManager()
        val revision = assets.revision.value
        manager().restore()
        testScope.advanceUntilIdle()
        assertEquals(id, state.project.backgroundAssetId)
        val restored = requireNotNull(assets.getAsset(id))
        assertEquals(8, restored.width)
        assertEquals(6, restored.height)
        assertEquals(android.graphics.Color.MAGENTA, restored.getPixel(0, 0))
        assertTrue(assets.revision.value > revision)
        assertTrue(errors.isEmpty())
    }

    @Test fun `missing or corrupt saved background reports an error without deleting metadata`() {
        manager().handle(TimelineIntent.Background.SetImage(bitmap()))
        testScope.advanceUntilIdle()
        val id = requireNotNull(state.project.backgroundAssetId)
        val file = File(requireNotNull(state.project.assets[id]?.sourcePath))
        assets = ProjectAssetManager()
        file.delete()
        manager().restore()
        testScope.advanceUntilIdle()
        assertTrue(errors.last().contains("missing"))
        assertEquals(id, state.project.backgroundAssetId)
        assertNull(assets.getAsset(id))
        file.writeText("not an image")
        manager().restore()
        testScope.advanceUntilIdle()
        assertTrue(errors.last().contains("decoded"))
        assertEquals(id, state.project.backgroundAssetId)
    }

    @Test fun `failed import cannot publish a background that only exists in memory`() {
        storageDir.deleteRecursively()
        storageDir.writeText("blocks storage directory")
        manager().handle(TimelineIntent.Background.SetImage(bitmap()))
        testScope.advanceUntilIdle()
        assertNull(state.project.backgroundAssetId)
        assertTrue(errors.single().contains("not saved"))
    }

    @Test fun `restore cannot resurrect an image removed while decoding`() {
        manager().handle(TimelineIntent.Background.SetImage(bitmap()))
        testScope.advanceUntilIdle()
        val id = requireNotNull(state.project.backgroundAssetId)
        assets = ProjectAssetManager()
        val decoded = bitmap()
        lateinit var restoring: BackgroundManager
        restoring = BackgroundManager(
            testScope,
            testDispatcher,
            assets,
            storageDir,
            { state },
            { state = it(state) },
            decode = {
                restoring.handle(TimelineIntent.Background.Remove)
                decoded
            },
        )
        restoring.restore()
        testScope.advanceUntilIdle()
        assertNull(state.project.backgroundAssetId)
        assertNull(assets.getAsset(id))
        assertTrue(decoded.isRecycled)
    }

    @Test fun `cache release does not recycle pixels still held by a renderer`() {
        val image = bitmap()
        assets.registerAsset("background-held", image)
        assets.releaseAsset("background-held")
        assertNull(assets.getAsset("background-held"))
        assertFalse(image.isRecycled)
        assets.registerAsset("background-held", image)
        assets.clear()
        assertNull(assets.getAsset("background-held"))
        assertFalse(image.isRecycled)
    }

    @Test fun `superseded import followed by failed import preserves the previous background`() {
        val original = bitmap()
        manager().handle(TimelineIntent.Background.SetImage(original))
        testScope.advanceUntilIdle()
        val originalId = requireNotNull(state.project.backgroundAssetId)
        val blocked = File(storageDir, "background-blocked.png").apply { mkdirs() }
        File(blocked, "keep").writeText("prevent directory replacement")
        val ids = listOf("superseded", "blocked").iterator()
        val importing = BackgroundManager(
            testScope,
            testDispatcher,
            assets,
            storageDir,
            { state },
            { state = it(state) },
            id = { ids.next() },
            onError = errors::add,
        )
        importing.handle(TimelineIntent.Background.SetImage(bitmap()))
        importing.handle(TimelineIntent.Background.SetImage(bitmap()))
        testScope.advanceUntilIdle()
        assertEquals(originalId, state.project.backgroundAssetId)
        assertTrue(assets.getAsset(originalId) === original)
        assertTrue(File(requireNotNull(state.project.assets[originalId]?.sourcePath)).isFile)
        assertFalse(File(storageDir, "background-superseded.png").exists())
        assertTrue(errors.single().contains("not saved"))
    }

    @Test fun `replacement and removal retain files still referenced by the last saved document`() {
        val importing = manager()
        importing.handle(TimelineIntent.Background.SetImage(bitmap()))
        testScope.advanceUntilIdle()
        val savedProject = state.project
        val originalId = requireNotNull(savedProject.backgroundAssetId)
        val originalFile = File(requireNotNull(savedProject.assets[originalId]?.sourcePath))
        importing.handle(TimelineIntent.Background.SetImage(bitmap()))
        testScope.advanceUntilIdle()
        val secondId = requireNotNull(state.project.backgroundAssetId)
        val secondFile = File(requireNotNull(state.project.assets[secondId]?.sourcePath))
        importing.handle(TimelineIntent.Background.Remove)
        testScope.advanceUntilIdle()
        assertTrue(originalFile.isFile)
        assertTrue(secondFile.isFile)
        state = TimelineState(project = savedProject)
        assets = ProjectAssetManager()
        manager().restore()
        testScope.advanceUntilIdle()
        assertTrue(assets.getAsset(originalId) != null)
        assertTrue(errors.isEmpty())
    }
}
