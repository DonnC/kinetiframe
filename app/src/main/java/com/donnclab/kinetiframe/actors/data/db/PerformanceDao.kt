package com.donnclab.kinetiframe.actors.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface PerformanceDao {
    @Query("SELECT * FROM performances WHERE projectId = :projectId ORDER BY displayName COLLATE NOCASE ASC")
    fun getForProject(projectId: String): List<PerformanceEntity>

    @Upsert
    fun upsert(entity: PerformanceEntity)

    @Query("DELETE FROM performances WHERE performanceId = :performanceId")
    fun deleteById(performanceId: String)
}
