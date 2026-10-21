package com.donnclab.kinetiframe.data.project

import com.donnclab.kinetiframe.engine.model.ProjectDocument
import com.donnclab.kinetiframe.models.Layer
import kotlinx.serialization.Serializable

/**
 * The full durable project state saved to document.json -- a superset of [ProjectDocument]
 * covering legacy per-frame layer-list snapshots ([frameLayers]) and explicit raster exposures.
 * Layer name/visibility/lock/opacity/order are project-wide. Pixel drawings are independent
 * unless [linkedCels] explicitly maps their frame positions to one stable shared cel ID.
 *
 * Frame *pixel* content is deliberately NOT part of this document -- it lives in
 * [com.donnclab.kinetiframe.engine.cache.FrameCacheManager]'s per-project "frames/" directory, which is
 * never evicted and is addressed by layer ID and frame index. Copying those PNGs into this JSON
 * would duplicate storage that already exists.
 *
 * Only structural, content-bearing state round-trips through this
 * document. Transient UI/view state -- canvas zoom/pan/rotation, active tool, onion-skin settings,
 * grid/ruler preferences, track-group collapse -- resets to its defaults on load, the same way
 * reopening a document in most editors does not restore exact viewport state.
 */
@Serializable
data class ProjectSaveDocument(
    val schemaVersion: Int = 1,
    val project: ProjectDocument,
    val frameLayers: Map<Int, List<Layer>>,
    val totalFrames: Int,
    val activeLayerIndex: Int = 0,
    /** layer ID -> (zero-based frame -> stable cel ID). Omitted by legacy projects. */
    val linkedCels: Map<String, Map<Int, String>> = emptyMap(),
)
