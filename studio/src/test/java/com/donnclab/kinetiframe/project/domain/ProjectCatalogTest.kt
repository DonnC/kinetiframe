package com.donnclab.kinetiframe.project.domain

import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.data.db.ProjectDao
import com.donnclab.kinetiframe.data.db.ProjectEntity
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProjectCatalogTest {
    @TempDir lateinit var root: File

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

    private val dao = FakeProjectDao()
    private var ids = 0
    private val catalog by lazy {
        ProjectCatalog(ProjectRepository(DocumentFileStore(root)), dao, now = { 5L }, newId = { "project-${++ids}" })
    }

    private fun <T> value(result: StoreResult<T>): T = (result as StoreResult.Value).value

    @Test fun `a created project is listed and indexed`() = runBlocking {
        val created = value(catalog.create("  My Cartoon ", StageDimensions.LandscapeHd, 12))

        assertEquals("My Cartoon", created.displayName)
        assertEquals(listOf("project-1"), value(catalog.list()).map { it.projectId })
        assertEquals("My Cartoon", dao.rows.getValue("project-1").title)
    }

    @Test fun `names are unique for create and rename`() = runBlocking {
        value(catalog.create("Cartoon", StageDimensions.LandscapeHd, 12))
        val other = value(catalog.create("Other", StageDimensions.LandscapeHd, 12))

        val duplicate = catalog.create("cartoon", StageDimensions.LandscapeHd, 12)
        val renamed = catalog.rename(other.projectId, " Cartoon")

        assertTrue((duplicate as StoreResult.Failure).message.contains("already exists"))
        assertTrue((renamed as StoreResult.Failure).message.contains("already exists"))
        assertEquals("Other", dao.rows.getValue(other.projectId).title)
    }

    @Test fun `renaming and deleting keep the index in step`() = runBlocking {
        val project = value(catalog.create("Cartoon", StageDimensions.LandscapeHd, 12))

        value(catalog.rename(project.projectId, "Short film"))
        assertEquals("Short film", dao.rows.getValue(project.projectId).title)

        value(catalog.delete(project.projectId))
        assertTrue(value(catalog.list()).isEmpty())
        assertTrue(dao.rows.isEmpty())
    }

    @Test fun `reconciling indexes missed projects and drops rows for missing ones`() = runBlocking {
        val project = value(catalog.create("Cartoon", StageDimensions.LandscapeHd, 12))
        dao.rows.clear()
        dao.rows["gone"] = indexRow("gone")

        catalog.reconcileIndex(value(catalog.list()))

        assertEquals(setOf(project.projectId), dao.rows.keys)
    }

    private fun indexRow(id: String) = ProjectEntity(
        projectId = id,
        title = id,
        createdAtEpochMs = 1L,
        lastModified = 1L,
        durationFrames = 1,
        fps = 12,
        resolutionWidth = 1,
        resolutionHeight = 1,
        thumbnailPath = null,
    )
}
