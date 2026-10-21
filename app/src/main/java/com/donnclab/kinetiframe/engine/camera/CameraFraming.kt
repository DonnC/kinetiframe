package com.donnclab.kinetiframe.engine.camera

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Touch deltas in unrotated stage space; dragging the image right moves the camera left. */
object CameraFraming {
    fun drag(pose: CameraPose, dx: Float, dy: Float, scale: Float, turn: Float, width: Int, height: Int): CameraPose {
        require(width > 0 && height > 0 && scale.isFinite() && scale > 0)
        val angle = pose.rotation * PI / 180
        val x = (cos(angle) * dx - sin(angle) * dy).toFloat()
        val y = (sin(angle) * dx + cos(angle) * dy).toFloat()
        return pose.copy(
            x = (pose.x - x / (pose.zoom * width)).coerceIn(-1f, 2f),
            y = (pose.y - y / (pose.zoom * height)).coerceIn(-1f, 2f),
            zoom = (pose.zoom * scale).coerceIn(.25f, 4f),
            // SceneCamera applies inverse camera rotation; keep the image following fingers.
            rotation = ((pose.rotation - turn + 540f) % 360f) - 180f,
        )
    }
}
