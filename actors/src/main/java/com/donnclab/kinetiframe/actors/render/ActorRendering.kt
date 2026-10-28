package com.donnclab.kinetiframe.actors.render

import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.data.storage.DocumentFileStore

/**
 * Draws the actors placed in a project, so screens can render them without holding the actor
 * stores. Each [newRenderer] call returns a renderer with its own caches; a screen keeps one for
 * as long as it shows actors and clears it when done.
 */
class ActorRendering(
    private val actors: ActorRepository,
    private val performances: PerformanceRepository,
    private val files: DocumentFileStore,
) {
    /** Checks a new placement can be drawn and fits it to the stage. */
    val placements = ActorPlacementPreparation(actors, performances, files)

    fun newRenderer(): ActorFrameRenderer = ActorFrameRenderer(actors, performances, files)
}
