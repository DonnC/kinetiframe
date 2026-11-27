package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.core.actor.document.ActorManifest
import com.donnclab.kinetiframe.core.actor.document.PerformanceDocument
import com.donnclab.kinetiframe.core.contracts.StoreResult

/**
 * The performances a timeline can place: every actor, each actor's performances across all
 * projects, and copying one into the project being edited. Screens use this instead of the actor
 * stores. Every function reads files; call them off the main thread.
 */
class PerformanceChoices(private val actors: ActorRepository, private val performances: PerformanceRepository) {
    fun actors(): StoreResult<List<ActorManifest>> = actors.list()

    fun performancesOf(actorId: String): StoreResult<List<PerformanceDocument>> = performances.listForActor(actorId)

    /** Copies [performance] into [projectId], or returns the project's existing copy. */
    fun copyIntoProject(performance: PerformanceDocument, projectId: String): StoreResult<PerformanceDocument> =
        performances.copyIntoProject(performance, projectId)
}
