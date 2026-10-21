package com.donnclab.kinetiframe.data.project

import com.donnclab.kinetiframe.engine.model.StageDimensions
import kotlinx.serialization.Serializable

/** Lightweight, gallery-listing-only metadata -- mirrors ActorManifest's manifest/document split
 *  (see ActorRepository) so listing every project for the gallery never has to parse a full
 *  [ProjectSaveDocument] (whose frameLayers map can be large for a long animation). The full
 *  document lives separately in document.json; this file is manifest.json. */
@Serializable
data class ProjectManifest(
    val schemaVersion: Int = 1,
    val projectId: String,
    val displayName: String,
    val stage: StageDimensions,
    val fps: Int,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    // Lets the project list show each duration without parsing the full ProjectSaveDocument.
    // The default keeps manifests written before this field existed readable.
    val totalFrames: Int = 1,
    /** True only for a Pack Creator scratch draft (see PackCreatorRoute) -- a real project
     *  directory under the same `projects/` root, reusing all the same frame-drawing machinery,
     *  but deliberately never surfaced in [com.donnclab.kinetiframe.data.project.ProjectRepository.list]'s
     *  gallery listing (or backfilled into the Room index) since it isn't a project a person
     *  would open from their gallery. Deleted outright once its pack is successfully bundled. */
    val isPackDraft: Boolean = false,
    /** Path to a small PNG preview of frame 0, if one has ever been generated (see EditorScreen's
     *  onSaveProjectClick and [ProjectRepository.thumbnailFile]) -- null means "no thumbnail yet,"
     *  not "no background." Like every other derived field in this app, a stale non-null path
     *  whose file no longer exists just means "regenerate it," never a corruption signal. */
    val thumbnailPath: String? = null,
) {
    fun validate(): ProjectManifest {
        require(projectId.isNotBlank()) { "Project ID cannot be blank" }
        require(displayName.isNotBlank()) { "Project name cannot be blank" }
        require(fps in 1..30) { "Project FPS must be between 1 and 30" }
        return this
    }
}
