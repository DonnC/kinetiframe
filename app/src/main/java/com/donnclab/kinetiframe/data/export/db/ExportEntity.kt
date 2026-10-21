package com.donnclab.kinetiframe.data.export.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One completed export of a project, indexed in Room purely as a fast "did I already export this,
 * and where did it land" lookup. Like every Room entity in this app, it is a rebuildable cache
 * over the real file on disk, never the source of truth: [filePath] pointing at a file that no longer exists just means the row is stale and
 * should be dropped, not that anything is corrupted.
 *
 * Keeping this as its own table rather than cramming fields onto [com.donnclab.kinetiframe.data.project.db.ProjectEntity]
 * matters because a project can be exported more than once (different presets, re-exports after
 * edits) -- a 1:1 field would either overwrite history or need its own array-in-a-column hack.
 */
@Entity(tableName = "exports")
data class ExportEntity(
    @PrimaryKey val exportId: String,
    val projectId: String,
    val filePath: String,
    val createdAtEpochMs: Long,
    val durationFrames: Int,
    val fps: Int,
    val widthPx: Int,
    val heightPx: Int,
    val fileSizeBytes: Long,
)
