package com.donnclab.kinetiframe.library.ui

import com.donnclab.kinetiframe.actors.data.ActorFileStore
import com.donnclab.kinetiframe.actors.data.ActorStoreResult
import com.donnclab.kinetiframe.actors.data.LibraryItemManifest
import com.donnclab.kinetiframe.actors.data.LibraryScope
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.library.data.PackItemEntry
import com.donnclab.kinetiframe.library.data.PackManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PackEditFlowTest {
    @TempDir lateinit var root: File
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach fun teardown() {
        Dispatchers.resetMain()
    }
    private fun <T> value(result: ActorStoreResult<T>): T = when (result) {
        is ActorStoreResult.Value -> result.value
        is ActorStoreResult.Failure -> error(result.message)
    }
    private fun save(
        repository: LibraryRepository,
        title: String,
        name: String,
        scope: LibraryScope = LibraryScope.PERSONAL,
        project: String? = null,
    ): LibraryItemManifest {
        val file = File(root, "source.png").apply { writeText(name) }
        return value(
            repository.exportPack(
                "faces",
                title,
                PackManifest(
                    title,
                    totalItems = 1,
                    canvasDimension = 128,
                    items = listOf(PackItemEntry("face.png", 0, name)),
                ),
                mapOf("face.png" to file),
                scope,
                project,
                tags = setOf("ls-faces"),
                nowEpochMs = 12,
            ),
        )
    }

    @Test fun `review edit emits real pack identity then refreshes edited inspector`() = runTest(dispatcher) {
        val files = ActorFileStore(root)
        val repository = LibraryRepository(files)
        val original = save(repository, "Faces", "Happy")
        val model = LibraryViewModel(LocalLibraryCatalog(files, repository), ioDispatcher = dispatcher)
        model.handleIntent(LibraryIntent.Reload)
        val loaded = model.state.first { !it.loading }
        model.handleIntent(LibraryIntent.OpenPackContents(loaded.packs.single()))
        model.state.first { !it.packLoading }
        model.handleIntent(LibraryIntent.EditOpenPack)
        assertEquals(LibraryEffect.EditPack(original), model.effect.first())
        save(repository, "New faces", "Surprised")
        model.handleIntent(LibraryIntent.Reload)
        val refreshed = model.state.first {
            !it.loading && !it.packLoading && it.openPackContents?.title == "New faces"
        }
        assertEquals("Surprised", refreshed.packContents.single().name)
        assertEquals(1, refreshed.packs.size)
        model.handleIntent(LibraryIntent.ClosePackContents)
    }

    @Test fun `reopening project pack preserves scope without overwriting personal pack`() {
        val repository = LibraryRepository(ActorFileStore(root))
        save(repository, "Personal", "Happy")
        val projectPack = save(repository, "Project", "Concerned", LibraryScope.PROJECT, "episode")
        val reopened = value(
            repository.loadPack(projectPack.libraryItemId, projectPack.scope, projectPack.sourceProjectId),
        ).first
        save(repository, "Edited project", "Angry", reopened.scope, reopened.sourceProjectId)
        assertEquals("Personal", value(repository.loadPack("faces")).first.displayName)
        val edited = value(LibraryRepository(ActorFileStore(root)).loadPack("faces", LibraryScope.PROJECT, "episode"))
        assertEquals("Edited project", edited.first.displayName)
        assertEquals("Angry", edited.second.items.single().name)
        assertEquals(setOf("ls-faces"), edited.first.tags)
    }
}
