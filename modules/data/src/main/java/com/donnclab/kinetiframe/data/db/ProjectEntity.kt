// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fast-loading gallery index row -- avoids scanning every project's .kframe files just to
 * populate the gallery. This table is a *cache*, never the source of truth -- [ProjectRepository]'s
 * manifest.json/document.json files on disk remain authoritative. If this table and the
 * filesystem ever disagree (a manual file copy, a failed write, an app update that predates this
 * table), [ProjectManagerViewModel]'s reload() re-derives this table from the filesystem rather
 * than trusting it blindly -- see that class's reindexing fallback.
 *
 * [createdAtEpochMs] mirrors what [com.donnclab.kinetiframe.core.document.ProjectManifest] already tracks,
 * for gallery sort-by-created. [thumbnailPath] is a frame-0 preview PNG path, regenerated on every
 * project save (see [com.donnclab.kinetiframe.data.project.ProjectRepository.thumbnailFile]) -- null means
 * no thumbnail has been generated yet, not that the project has no background.
 */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val projectId: String,
    val title: String,
    val createdAtEpochMs: Long,
    val lastModified: Long,
    val durationFrames: Int,
    val fps: Int,
    val resolutionWidth: Int,
    val resolutionHeight: Int,
    val thumbnailPath: String? = null,
)
