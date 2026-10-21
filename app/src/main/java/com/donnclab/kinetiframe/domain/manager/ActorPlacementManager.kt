package com.donnclab.kinetiframe.domain.manager

import com.donnclab.kinetiframe.engine.model.ActorPlacementReference
import com.donnclab.kinetiframe.engine.rigging.model.Transform
import com.donnclab.kinetiframe.ui.TimelineState
import java.util.UUID

/** Pure project-reference mutations; actor math and files remain outside the FxF editor. */
class ActorPlacementManager(
    private val getState: () -> TimelineState,
    private val updateState: ((TimelineState) -> TimelineState) -> Unit,
) {
    fun add(reference: ActorPlacementReference) {
        require(reference.placementId !in getState().project.actorPlacements) { "Placement already exists" }
        put(reference)
    }
    fun remove(placementId: String) = updateState { state ->
        state.copy(project = state.project.copy(actorPlacements = state.project.actorPlacements - placementId))
    }
    fun duplicate(placementId: String) {
        val original = getState().project.actorPlacements[placementId] ?: return
        val copy = original.copy(placementId = "placement-${UUID.randomUUID()}", startFrame = endFrame(original))
        add(copy)
    }
    fun updateTransform(placementId: String, transform: Transform) {
        val existing = getState().project.actorPlacements[placementId] ?: return
        updateState { state ->
            state.copy(
                project = state.project.copy(
                    actorPlacements =
                    state.project.actorPlacements + (placementId to existing.copy(transform = transform)),
                ),
            )
        }
    }

    /** Drag-trim from the main-timeline clip's start/end handles. Clamped defensively here too,
     *  even though the UI already clamps while dragging, since any caller could pass raw values. */
    fun trim(placementId: String, startFrame: Int, durationFrames: Int) {
        val existing = getState().project.actorPlacements[placementId] ?: return
        val trimmed = com.donnclab.kinetiframe.domain.actor.ActorPlacementTrimming.shorten(
            existing,
            startFrame,
            durationFrames,
        )
        put(trimmed)
    }
    fun move(placementId: String, startFrame: Int) {
        val existing = getState().project.actorPlacements[placementId] ?: return
        put(existing.copy(startFrame = startFrame.coerceAtLeast(0)))
    }

    /** Inline switch on an already-placed clip: the actor stays fixed, only which saved
     *  performance plays (and the clip's resulting duration) changes. Start frame and transform
     *  are left untouched -- this is not a re-placement. */
    fun switchPerformance(
        placementId: String,
        performanceId: String,
        actorRevisionId: String,
        label: String,
        durationFrames: Int,
    ) {
        val existing = getState().project.actorPlacements[placementId] ?: return
        val safeDuration = durationFrames.coerceAtLeast(1)
        put(
            existing.copy(
                performanceId = performanceId,
                actorRevisionId = actorRevisionId,
                label = label,
                durationFrames = safeDuration,
                sourceOffsetFrames = 0,
                sourceLoopFrames = 0,
                sourceSpanFrames = 0,
            ),
        )
    }

    /** Cuts a placed clip into two independent placements at [atFrame] (must fall strictly
     *  inside the clip's active range -- not at either edge, since that would just leave one
     *  half empty). The original [placementId] is kept for the earlier half so any existing
     *  reference to it (library export, an in-flight edit) stays valid; the later half gets a
     *  fresh id. Both halves are forced to `repeatCount = 1`: the timing model computes a
     *  placement's active span as `durationFrames * repeatCount`, so a split piece's own
     *  duration only means what it says if repeats aren't also multiplying it. Splitting a looped
     *  clip therefore does not divide its loop count between the halves. Source offsets retain the original playback phase in both
     *  pieces. Returns the new placement's id, or null if
     *  [placementId] doesn't exist or [atFrame] isn't strictly inside its range. */
    fun split(placementId: String, atFrame: Int): String? {
        val existing = getState().project.actorPlacements[placementId] ?: return null
        val totalFrames = existing.durationFrames.toLong() * existing.repeatCount
        val end = existing.startFrame + totalFrames
        if (atFrame <= existing.startFrame || atFrame >= end) return null
        val firstDuration = (atFrame - existing.startFrame).toInt()
        val secondDuration = (end - atFrame).toInt()
        val second = com.donnclab.kinetiframe.domain.actor.ActorPlacementTrimming.shorten(
            existing,
            atFrame,
            secondDuration,
        ).copy(
            placementId = "placement-${UUID.randomUUID()}",
            startFrame = atFrame,
            durationFrames = secondDuration,
            repeatCount = 1,
        )
        val first = com.donnclab.kinetiframe.domain.actor.ActorPlacementTrimming.shorten(
            existing,
            existing.startFrame,
            firstDuration,
        )
        updateState { state ->
            state.copy(
                project = state.project.copy(
                    actorPlacements =
                    state.project.actorPlacements + (placementId to first) + (second.placementId to second),
                ),
            )
        }
        return second.placementId
    }

    fun bringToFront(placementId: String) = updateZIndex(placementId) { placements ->
        (placements.maxOfOrNull { it.zIndex } ?: 0) + 1
    }
    fun sendToBack(placementId: String) = updateZIndex(placementId) { placements ->
        (placements.minOfOrNull { it.zIndex } ?: 0) - 1
    }

    private fun updateZIndex(placementId: String, target: (Collection<ActorPlacementReference>) -> Int) {
        val existing = getState().project.actorPlacements[placementId] ?: return
        val zIndex = target(getState().project.actorPlacements.values)
        updateState { state ->
            state.copy(
                project = state.project.copy(
                    actorPlacements =
                    state.project.actorPlacements + (placementId to existing.copy(zIndex = zIndex)),
                ),
            )
        }
    }

    /** Grow the scene to the exclusive clip end, without allocating or baking raster cels.
     * Removing/shortening a clip never destroys drawings or shrinks the artist's scene. */
    private fun put(reference: ActorPlacementReference) {
        val end = endFrame(reference)
        updateState { state ->
            state.copy(
                animation = state.animation.copy(totalFrames = maxOf(state.animation.totalFrames, end)),
                project = state.project.copy(
                    actorPlacements =
                    state.project.actorPlacements + (reference.placementId to reference),
                ),
            )
        }
    }

    private fun endFrame(reference: ActorPlacementReference): Int {
        val end = reference.startFrame.toLong() + reference.durationFrames.toLong() * reference.repeatCount
        require(end in 1..Int.MAX_VALUE.toLong()) { "Actor clip exceeds the timeline range" }
        return end.toInt()
    }
}
