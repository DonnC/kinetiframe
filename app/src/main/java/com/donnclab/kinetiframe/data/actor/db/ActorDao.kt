package com.donnclab.kinetiframe.data.actor.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/** Plain (non-suspend) queries, matching [com.donnclab.kinetiframe.data.actor.ActorRepository]'s own
 *  fully-synchronous style -- every caller already runs it under `Dispatchers.IO`, same as every
 *  disk call this repository makes. */
@Dao
interface ActorDao {
    @Query("SELECT * FROM actors ORDER BY displayName COLLATE NOCASE ASC")
    fun getAll(): List<ActorEntity>

    @Upsert
    fun upsert(entity: ActorEntity)

    @Query("DELETE FROM actors WHERE actorId = :actorId")
    fun deleteById(actorId: String)
}
