package com.donnclab.kinetiframe.export.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ExportDao {
    @Query("SELECT * FROM exports WHERE projectId = :projectId ORDER BY createdAtEpochMs DESC")
    suspend fun getForProject(projectId: String): List<ExportEntity>

    @Upsert
    suspend fun upsert(entity: ExportEntity)

    @Query("DELETE FROM exports WHERE exportId = :exportId")
    suspend fun deleteById(exportId: String)

    @Query("DELETE FROM exports WHERE projectId = :projectId")
    suspend fun deleteForProject(projectId: String)
}
