package com.donnclab.kinetiframe.media.video

data class ImportedVideoMetadata(
    val durationMicros: Long,
    val width: Int,
    val height: Int,
    val hasAudio: Boolean,
    val peaks: List<Float>,
)
