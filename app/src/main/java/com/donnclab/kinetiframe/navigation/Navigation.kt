package com.donnclab.kinetiframe.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation keys for KinetiFrame.
 */
@Serializable
data object ProjectGalleryKey : NavKey

@Serializable
data class EditorKey(val projectId: String? = null) : NavKey

/** A fresh, unique id per navigation (not reused) so two separate Pack Creator visits are always
 *  distinct destinations -- a singleton key here would let navigation collapse a brand-new pack
 *  session onto whatever composition state (draft id, tags, canvas size) a previous visit left
 *  behind.
 *
 *  [editItemId] is null to start a new pack (from Home's Packs tab button) and set to an existing
 *  PACK library item's id to reopen and edit it. */
@Serializable
data class PackCreatorKey(
    val sessionId: String = java.util.UUID.randomUUID().toString(),
    val editItemId: String? = null,
    val editScope: String = "PERSONAL",
    val sourceProjectId: String? = null,
) : NavKey

/** Renders the MP4 export for one project: progress, then a choice to save the video to the
 *  gallery or share it. */
@Serializable
data class ExportKey(val projectId: String) : NavKey

/** Full-screen Library browse -- reached from the Home bottom nav's "Library" tab. Distinct from
 *  [com.donnclab.kinetiframe.presentation.library.LibraryPickerDialog], which stays a modal picker used only when
 *  importing an asset into the current project or a new one. */
@Serializable
data object LibraryKey : NavKey

@Serializable
data object SettingsKey : NavKey

@Serializable
data class ActorStudioKey(
    val actorId: String,
    val projectId: String? = null,
    val performanceId: String? = null,
    /** Non-null only when Studio was opened by a concrete FxF placement edit. */
    val returnPlacementId: String? = null,
) : NavKey
