package com.donnclab.kinetiframe.ui.actors

import com.donnclab.kinetiframe.data.actor.ActorArtworkStore
import com.donnclab.kinetiframe.data.actor.ActorManifest
import com.donnclab.kinetiframe.data.actor.ActorRepository
import com.donnclab.kinetiframe.data.actor.ActorRevision
import com.donnclab.kinetiframe.data.actor.PerformanceDocument
import com.donnclab.kinetiframe.data.actor.PerformanceRepository
import com.donnclab.kinetiframe.engine.model.PuppetDefinition
import com.donnclab.kinetiframe.engine.model.PuppetInstance
import com.donnclab.kinetiframe.engine.rigging.texture.RigAssetManager

/**
 * Which document an Actor Studio session is opened against.
 *
 * These four identifiers travel together everywhere -- they are jointly what makes a session
 * unique (the same actor opened for rig setup, and opened again to author a performance inside a
 * project, are genuinely different documents and must never share engine state). Grouping them
 * keeps that invariant visible in one place instead of re-threading four positional strings
 * through every layer, and keeps the screen inside the 4-parameter budget.
 */
data class ActorStudioTarget(
    val actorId: String,
    val projectId: String? = null,
    val performanceId: String? = null,
    /** Set when the session was opened to edit an existing placement on a project timeline, which
     *  makes archiving the underlying performance unsafe until that placement is removed. */
    val returnPlacementId: String? = null,
)

/** The three stores an Actor Studio session reads and writes through, bundled so the screen takes
 *  a single collaborator parameter rather than one per repository. */
data class ActorStudioRepositories(
    val actors: ActorRepository,
    val performances: PerformanceRepository,
    val artwork: ActorArtworkStore,
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
