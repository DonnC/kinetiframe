package com.donnclab.kinetiframe.project.domain

import android.graphics.Bitmap
import android.graphics.Color
import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.ProjectManifest
import com.donnclab.kinetiframe.core.document.ProjectSaveDocument
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.data.db.ProjectDao
import com.donnclab.kinetiframe.data.db.ProjectEntity
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
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
class ProjectSaverTest {
    @get:Rule val folder = TemporaryFolder()

    private class FakeProjectDao : ProjectDao {
        val rows = linkedMapOf<String, ProjectEntity>()
        override suspend fun getAll() = rows.values.toList()
        override suspend fun getById(projectId: String) = rows[projectId]
        override suspend fun upsert(entity: ProjectEntity) {
            rows[entity.projectId] = entity
        }
        override suspend fun deleteById(projectId: String) {
            rows.remove(projectId)
        }
        override suspend fun clear() = rows.clear()
    }

    @Test fun `a save keeps the project's name and writes frames, thumbnail and index`() = runBlocking {
        val files = DocumentFileStore(folder.newFolder("store"))
        val projects = ProjectRepository(files)
        val dao = FakeProjectDao()
        val saver = ProjectSaver(files, projects, ActorRepository(files), PerformanceRepository(files), dao)
        val layer = Layer(name = "Main Layer", id = "main")
        val stage = StageDimensions(64, 48)
        val document = ProjectSaveDocument(
            project = ProjectDocument(id = "project-a", stage = stage, fps = 12, layers = listOf(layer)),
            frameLayers = mapOf(0 to listOf(layer), 1 to listOf(layer)),
            totalFrames = 2,
        )
        val manifest = ProjectManifest(
            projectId = "project-a",
            displayName = "My Cartoon",
            stage = stage,
            fps = 12,
            createdAtEpochMs = 1L,
            updatedAtEpochMs = 1L,
            totalFrames = 1,
        )
        assertTrue(projects.save(manifest, document.copy(totalFrames = 1)) is StoreResult.Value)

        val draft = folder.newFolder("draft")
        val saved = coroutineScope {
            val frames = FrameCacheManager(draft, this)
            frames.putFrame(
                "main",
                0,
                Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(Color.RED)
                },
            )
            frames.persistAllToDisk(listOf("main"), 2)
            saver.save("project-a", document, draft, time = 9L, frames = frames)
        }

        val written = (saved as StoreResult.Value).value
        assertEquals("My Cartoon", written.displayName)
        assertEquals(2, written.totalFrames)
        assertEquals(9L, written.updatedAtEpochMs)
        val projectFrames = File(files.projectDirectory("project-a"), "frames")
        assertTrue(projectFrames.walkTopDown().any { it.isFile && it.extension == "png" })
        assertTrue(projects.thumbnailFile("project-a").isFile)
        val row = dao.rows.getValue("project-a")
        assertEquals("My Cartoon", row.title)
        assertEquals(2, row.durationFrames)
        assertEquals(projects.thumbnailFile("project-a").absolutePath, row.thumbnailPath)
    }
}
