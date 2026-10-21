package com.donnclab.kinetiframe.engine.rigging.presets

internal object LegacyPresetChannelValidationReference {
    fun validate(channels: List<RotationChannel>, schemaVersion: Int, loop: Boolean) {
        channels.forEach {
            require(it.boneId.isNotBlank())
            require(
                listOf(it.offsetDegrees, it.amplitudeDegrees, it.cyclesPerLoop, it.phaseDegrees).all(Float::isFinite),
            )
            require(it.cyclesPerLoop in 0f..20f && kotlin.math.abs(it.amplitudeDegrees) <= 180f)
            require(kotlin.math.abs(it.offsetDegrees) <= 360f && kotlin.math.abs(it.phaseDegrees) <= 360f)
            if (schemaVersion ==
                1
            ) {
                require(it.points.isEmpty() && it.counterRotationBones.isEmpty()) {
                    "Authored curves require schema 2"
                }
            }
            if (schemaVersion >= 2) {
                require(it.points.size in 2..64) { "A curve needs 2..64 ordered points" }
                require(it.points.first().progress == 0f && it.points.last().progress == 1f)
                require(
                    it.points.all { p ->
                        p.progress.isFinite() && p.progress in 0f..1f && p.degrees.isFinite() &&
                            kotlin.math.abs(p.degrees) <= 180f
                    },
                )
                require(it.points.zipWithNext().all { (a, b) -> b.progress > a.progress })
                if (loop) require(it.points.first().degrees == it.points.last().degrees) { "Loop endpoints must match" }
                require(it.counterRotationBones.distinct().size == it.counterRotationBones.size)
                require(
                    it.counterRotationBones.all { id ->
                        id != it.boneId &&
                            channels.any { source -> source.boneId == id && source.counterRotationBones.isEmpty() }
                    },
                ) {
                    "Counter-rotation must reference uncompensated channels from this recipe"
                }
            }
        }
    }
}
