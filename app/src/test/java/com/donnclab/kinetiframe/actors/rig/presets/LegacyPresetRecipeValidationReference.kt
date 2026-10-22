package com.donnclab.kinetiframe.actors.rig.presets

import kotlin.math.abs

internal object LegacyPresetRecipeValidationReference {
    fun validate(recipe: ActorMotionPreset) = with(recipe) {
        require(schemaVersion in 1..3) { "Unsupported motion preset schema: $schemaVersion" }
        require(id.matches(Regex("[a-z0-9_-]{1,80}")) && name.isNotBlank())
        require(durationSeconds.isFinite() && durationSeconds in 0.25f..30f)
        require(channels.isNotEmpty() && channels.size <= 128)
        require(channels.map { it.boneId }.distinct().size == channels.size)
        require(requiredBones.isNotEmpty() && requiredBones.distinct().size == requiredBones.size)
        require(schemaVersion == 3 || rootMotion.isEmpty()) { "Root motion requires preset schema 3" }
        require(rootMotion.size <= 16 && rootMotion.map { it.boneId }.distinct().size == rootMotion.size)
        rootMotion.forEach { channel ->
            require(channel.boneId in requiredBones && channel.points.size in 2..64)
            require(channel.points.first().progress == 0f && channel.points.last().progress == 1f)
            require(
                channel.points.all {
                    it.progress.isFinite() && it.progress in 0f..1f && it.x.isFinite() &&
                        it.y.isFinite() &&
                        abs(it.x) <= 4 &&
                        abs(it.y) <= 4
                },
            )
            require(channel.points.zipWithNext().all { (a, b) -> b.progress - a.progress >= .000001f })
            if (loop) {
                require(
                    channel.points.first().x == channel.points.last().x &&
                        channel.points.first().y == channel.points.last().y,
                )
            }
        }
        LegacyPresetChannelValidationReference.validate(channels, schemaVersion, loop)
    }
}
