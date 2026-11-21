package com.donnclab.kinetiframe.presentation.actors

import com.donnclab.kinetiframe.actors.rig.texture.RigAssetManager
import com.donnclab.kinetiframe.core.actor.document.ActorManifest
import com.donnclab.kinetiframe.core.actor.document.ActorRevision
import com.donnclab.kinetiframe.core.actor.document.PerformanceDocument
import com.donnclab.kinetiframe.core.document.PuppetDefinition
import com.donnclab.kinetiframe.core.document.PuppetInstance

/**
 * Which document an Actor Studio session is opened against.
 *
 * Together these identifiers make a session unique. The same actor opened for rig setup and
 * opened to author a performance inside a project are different documents and must never share
 * engine state. Grouping them keeps that rule in one place instead of passing four positional
 * strings through the screen and its ViewModel.
 */
data class ActorStudioTarget(
    val actorId: String,
    val projectId: String? = null,
    val performanceId: String? = null,
    /** Set when the session was opened to edit an existing placement on a project timeline, which
     *  makes archiving the underlying performance unsafe until that placement is removed. */
    val returnPlacementId: String? = null,
)

/** A loaded actor document: the actor itself, the immutable revision being edited, and the
 *  performance layered on top when the session is authoring one. */
data class ActorStudioDocumentSnapshot(
    val manifest: ActorManifest,
    val revision: ActorRevision,
    val performance: PerformanceDocument?,
)

/** What the restored canvas reports back up to the Actor Studio session. */
data class ActorStudioEditorCallbacks(
    val onSaveActorRevision: (PuppetDefinition, RigAssetManager) -> Unit,
    /** Gesture data and duration are saved together; neither belongs to the actor revision. */
    val onSavePerformance: (PuppetInstance, Long) -> Unit,
    val onPerformanceActions: (() -> Unit)?,
    val onExit: () -> Unit,
    val onDirtyChanged: (Boolean) -> Unit,
    val onSaveActionAvailable: (((() -> Unit)?) -> Unit)? = null,
    val onSavePreparationFailed: (() -> Unit)? = null,
)
