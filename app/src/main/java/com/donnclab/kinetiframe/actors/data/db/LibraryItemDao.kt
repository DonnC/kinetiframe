package com.donnclab.kinetiframe.actors.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface LibraryItemDao {
    /** [projectId] is only applied as a filter when non-null -- pass null for PERSONAL-scope
     *  listings, where every row matching [scope] belongs regardless of provenance. */
    @Query(
        "SELECT * FROM library_items WHERE scope = :scope AND (:projectId IS NULL OR sourceProjectId = :projectId) ORDER BY createdAtEpochMs DESC",
    )
    fun list(scope: String, projectId: String?): List<LibraryItemEntity>

    @Upsert
    fun upsert(entity: LibraryItemEntity)

    @Query("DELETE FROM library_items WHERE libraryItemId = :libraryItemId")
    fun deleteById(libraryItemId: String)
}
