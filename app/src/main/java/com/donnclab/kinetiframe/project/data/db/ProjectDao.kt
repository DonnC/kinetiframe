package com.donnclab.kinetiframe.project.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert

/**
 * Suspend-function DAO, deliberately not exposing Room's Flow reactivity -- every other
 * repository in this codebase (ActorRepository, ProjectRepository) is a plain synchronous/suspend
 * call wrapped in `withContext(Dispatchers.IO)` by its ViewModel, and this index follows the same
 * shape rather than introducing a second, inconsistent reactive-query style alongside it.
 */
@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastModified DESC")
    suspend fun getAll(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE projectId = :projectId LIMIT 1")
    suspend fun getById(projectId: String): ProjectEntity?

    @Upsert
    suspend fun upsert(entity: ProjectEntity)

    @Query("DELETE FROM projects WHERE projectId = :projectId")
    suspend fun deleteById(projectId: String)

    @Query("DELETE FROM projects")
    suspend fun clear()
}
