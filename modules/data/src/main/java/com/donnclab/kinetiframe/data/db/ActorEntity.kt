// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fast-loading actor dashboard index -- a cache over [com.donnclab.kinetiframe.actors.data.ActorRepository]'s
 * real, authoritative `actors/<id>/manifest.json` files, same philosophy as
 * [com.donnclab.kinetiframe.data.db.ProjectEntity]: never trusted blindly, rebuilt from disk
 * whenever it comes back empty. [thumbnailPath] mirrors what
 * [com.donnclab.kinetiframe.actors.data.ActorRepository.thumbnailPath] would resolve by walking the current
 * revision's artwork files, computed once at write time instead of on every dashboard load.
 */
@Entity(tableName = "actors")
data class ActorEntity(
    @PrimaryKey val actorId: String,
    val displayName: String,
    val templateId: String,
    val currentRevisionId: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val thumbnailPath: String? = null,
)
