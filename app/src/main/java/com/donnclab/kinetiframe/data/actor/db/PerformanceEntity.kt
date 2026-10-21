package com.donnclab.kinetiframe.data.actor.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fast-loading index of one project's actor performances -- a cache over
 * [com.donnclab.kinetiframe.data.actor.PerformanceRepository]'s real `performances/<id>.json` files.
 * Those files also carry pose keys and rotation curves, which a picker or track lane never needs
 * just to show a name and duration; this row is the lightweight subset, so listing performances
 * to populate a picker no longer means parsing every performance's full animation data first.
 */
@Entity(tableName = "performances")
data class PerformanceEntity(
    @PrimaryKey val performanceId: String,
    val projectId: String,
    val actorId: String,
    val actorRevisionId: String,
    val displayName: String,
    val durationMicros: Long,
    val playback: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)
