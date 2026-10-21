package com.donnclab.kinetiframe.domain.actor

import com.donnclab.kinetiframe.data.actor.ActorPlacement
import com.donnclab.kinetiframe.data.actor.ActorRepository
import com.donnclab.kinetiframe.data.actor.ActorRevision
import com.donnclab.kinetiframe.data.actor.ActorStoreResult
import com.donnclab.kinetiframe.data.actor.PerformanceDocument
import com.donnclab.kinetiframe.data.actor.PerformanceRepository

/** Resolves a placement once into render input; the renderer receives no file-system handles. */
class ResolveActorRenderUseCase(
    private val actors: ActorRepository,
    private val performances: PerformanceRepository,
    private val loadPerformance: (String, String) -> ActorStoreResult<PerformanceDocument> = performances::load,
    private val loadRevision: (String, String) -> ActorStoreResult<ActorRevision> = actors::loadRevision,
) {
    fun execute(placement: ActorPlacement, frame: Int, fps: Int): ActorStoreResult<ActorRenderInput> {
        val micros = ActorPlacementTiming.elapsedMicros(placement, frame, fps)
        if (micros == null) {
            return ActorStoreResult.Failure("Placement is not active at frame $frame")
        }
        val performance = when (val result = loadPerformance(placement.projectId, placement.performanceId)) {
            is ActorStoreResult.Value -> result.value
            is ActorStoreResult.Failure -> return result
        }
        val revision = when (val result = loadRevision(performance.actorId, performance.actorRevisionId)) {
            is ActorStoreResult.Value -> result.value
            is ActorStoreResult.Failure -> return result
        }
        return ActorStoreResult.Value(
            ActorRenderInput(
                placement,
                revision,
                performance,
                PerformanceEvaluator.rotations(performance, micros, revision.sceneGraph),
                PerformanceEvaluator.pose(performance, micros, revision.sceneGraph),
            ),
        )
    }
}

data class ActorRenderInput(
    val placement: ActorPlacement,
    val revision: ActorRevision,
    val performance: PerformanceDocument,
    val rotations: Map<String, Float>,
    val posedGraph: com.donnclab.kinetiframe.engine.rigging.model.SceneGraph = revision.sceneGraph,
)
