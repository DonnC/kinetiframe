package com.donnclab.kinetiframe.video

internal data class ImportedVideoMetadata(
    val durationMicros: Long,
    val width: Int,
    val height: Int,
    val hasAudio: Boolean,
    val peaks: List<Float>,
)
