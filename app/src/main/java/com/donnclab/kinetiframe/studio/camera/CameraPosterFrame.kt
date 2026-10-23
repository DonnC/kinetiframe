package com.donnclab.kinetiframe.studio.camera

import com.donnclab.kinetiframe.core.document.camera.CameraShot

/** Gallery artwork is a representative scene, not the black opening of an intentional fade. */
object CameraPosterFrame {
    fun select(shots: List<CameraShot>, totalFrames: Int, fps: Int): Int {
        require(totalFrames > 0 && fps > 0)
        return (0 until totalFrames).firstOrNull { SceneCamera.evaluate(shots, it, fps).black <= .02f } ?: 0
    }
}
