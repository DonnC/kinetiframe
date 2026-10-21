package com.donnclab.kinetiframe.engine.model

/** Export-safe guides expressed in document coordinates, never device pixels. */
enum class SafeFrameRatio(val width: Float, val height: Float) {
    WIDESCREEN_16_9(16f, 9f),
    SQUARE_1_1(1f, 1f),
    PORTRAIT_4_5(4f, 5f),
}

data class StageRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

fun StageDimensions.safeFrame(ratio: SafeFrameRatio): StageRect {
    val stageRatio = width.toFloat() / height
    val requested = ratio.width / ratio.height
    val frameWidth: Float
    val frameHeight: Float
    if (stageRatio > requested) {
        frameHeight = height.toFloat()
        frameWidth = frameHeight * requested
    } else {
        frameWidth = width.toFloat()
        frameHeight = frameWidth / requested
    }
    return StageRect(
        (width - frameWidth) / 2f,
        (height - frameHeight) / 2f,
        (width + frameWidth) / 2f,
        (height + frameHeight) / 2f,
    )
}
