package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.data.ActorPlacement
import com.donnclab.kinetiframe.actors.data.PerformanceDocument
import com.donnclab.kinetiframe.project.model.ActorPlacementReference

/** Maps project UI references to compact durable documents without duplicating rig mathematics. */
object ActorPlacementMapper {
    fun document(projectId: String, reference: ActorPlacementReference): ActorPlacement = ActorPlacement(
        placementId = reference.placementId,
        projectId = projectId,
        performanceId = reference.performanceId,
        startFrame = reference.startFrame,
        durationFrames = reference.durationFrames,
        repeatCount = reference.repeatCount,
        transform = reference.transform,
        zIndex = reference.zIndex,
        label = reference.label,
        sourceOffsetFrames = reference.sourceOffsetFrames,
        sourceLoopFrames = reference.sourceLoopFrames,
        sourceSpanFrames = reference.sourceSpanFrames,
        selectionTransform = reference.selectionTransform,
    ).validate()

    fun reference(document: ActorPlacement, performance: PerformanceDocument): ActorPlacementReference {
        require(document.projectId == performance.projectId) {
            "Placement and performance belong to different projects"
        }
        require(document.performanceId == performance.performanceId) { "Placement references a different performance" }
        return ActorPlacementReference(
            placementId = document.placementId,
            performanceId = performance.performanceId,
            actorId = performance.actorId,
            actorRevisionId = performance.actorRevisionId,
            label = document.label ?: performance.displayName,
            startFrame = document.startFrame,
            durationFrames = document.durationFrames,
            repeatCount = document.repeatCount,
            zIndex = document.zIndex,
            transform = document.transform,
            sourceOffsetFrames = document.sourceOffsetFrames,
            sourceLoopFrames = document.sourceLoopFrames,
            sourceSpanFrames = document.sourceSpanFrames,
            selectionTransform = document.selectionTransform,
        )
    }
}
