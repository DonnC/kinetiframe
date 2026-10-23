package com.donnclab.kinetiframe.studio.camera

import com.donnclab.kinetiframe.core.document.camera.CameraAction
import com.donnclab.kinetiframe.core.document.camera.CameraEasing
import com.donnclab.kinetiframe.core.document.camera.CameraPose
import com.donnclab.kinetiframe.core.document.camera.CameraShot
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/** Portable recipes retain timing in seconds; reusing at another FPS preserves the feel. */
@Serializable
data class CameraRecipe(
    val name: String,
    val shot: CameraShot,
    val fps: Int,
    val referencePose: CameraPose = CameraPose(),
) {
    init {
        require(name.isNotBlank() && name.length <= 80 && fps > 0)
    }
    fun place(id: String, from: Int, to: Int, base: CameraPose): CameraShot {
        fun relative(p: CameraPose) = CameraPose(
            (base.x + p.x - referencePose.x).coerceIn(-1f, 2f),
            (base.y + p.y - referencePose.y).coerceIn(-1f, 2f),
            (base.zoom * p.zoom / referencePose.zoom).coerceIn(.25f, 4f),
            (base.rotation + p.rotation - referencePose.rotation).coerceIn(-180f, 180f),
            p.black,
        )
        return shot.copy(
            id = id,
            startFrame = from,
            endFrame = to,
            start = relative(shot.start),
            end = relative(shot.end),
        )
    }
    fun durationFrames(targetFps: Int) =
        ((shot.endFrame - shot.startFrame).toDouble() * targetFps / fps).roundToInt().coerceAtLeast(1)
    fun retime(targetFps: Int) = copy(
        shot = shot.copy(
            startFrame = 0,
            endFrame = durationFrames(targetFps),
            waitFrames = (shot.waitFrames.toDouble() * targetFps / fps).roundToInt(),
            settleFrames = (shot.settleFrames.toDouble() * targetFps / fps).roundToInt(),
        ),
        fps = targetFps,
    )
}

object CameraRecipes {
    fun builtIn(fps: Int): List<CameraRecipe> {
        fun recipe(
            name: String,
            action: CameraAction,
            strength: Float,
            seconds: Int = 2,
            transform: (CameraShot) -> CameraShot = { it },
        ) = CameraRecipe(
            name,
            transform(SceneCamera.preset(name, action, 0, fps * seconds, CameraPose(), strength)),
            fps,
        )
        return listOf(
            recipe("Dramatic reveal", CameraAction.PULL_OUT, .8f, 3) {
                it.copy(
                    start = CameraPose(zoom = 1.8f),
                    end = CameraPose(),
                    waitFrames =
                    fps / 2,
                    settleFrames = fps / 2,
                )
            },
            recipe("Comic impact", CameraAction.SHAKE, .9f, 1) { it.copy(frequencyHz = 18f, rotationStrength = .4f) },
            recipe("Suspense push", CameraAction.PUSH_IN, .4f, 4) {
                it.copy(waitFrames = fps, easing = CameraEasing.EASE_IN)
            },
            recipe("Energetic pan", CameraAction.WHIP_PAN, .8f),
            recipe("Documentary handheld", CameraAction.HANDHELD, .5f, 4) {
                it.copy(frequencyHz = .8f, rotationStrength = .15f)
            },
        )
    }
}
