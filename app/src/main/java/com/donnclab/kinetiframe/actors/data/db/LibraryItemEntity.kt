package com.donnclab.kinetiframe.actors.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fast-loading index of one library/vault listing -- a cache over
 * [com.donnclab.kinetiframe.library.data.LibraryRepository]'s real `<root>/<itemId>/manifest.json` files,
 * covering every kind that repository lists (audio, background image, frame, pack, saved
 * project, actor, performance, bundle) in one table, since they already share one manifest shape
 * on disk. [scope]/[sourceProjectId] together are what a listing filters by: PERSONAL rows ignore
 * [sourceProjectId] as a filter, PROJECT rows are scoped to the one project that physically holds
 * them. [tags] is stored comma-joined rather than in a join table because nothing queries tags
 * across the whole library; add a table if tag search is introduced.
 */
@Entity(tableName = "library_items")
data class LibraryItemEntity(
    @PrimaryKey val libraryItemId: String,
    val kind: String,
    val displayName: String,
    val sourceActorId: String? = null,
    val sourcePerformanceId: String? = null,
    val sourceProjectId: String? = null,
    val scope: String,
    val payloadFileName: String? = null,
    val tags: String = "",
    val license: String? = null,
    val attribution: String? = null,
    val createdAtEpochMs: Long,
)
