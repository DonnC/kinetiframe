package com.donnclab.kinetiframe.core.document

/** Immutable queue envelope. Only its owned point snapshot may be replayed after suspension. */
class StrokeSubmission private constructor(val command: DrawCommand, val frameIndex: Int, val layerId: String) {
    companion object {
        fun capture(command: DrawCommand, frameIndex: Int, layerId: String): StrokeSubmission {
            require(frameIndex >= 0)
            require(layerId.isNotBlank())
            return StrokeSubmission(
                command.copy(points = command.points.toList()),
                frameIndex,
                layerId,
            )
        }
    }
}
