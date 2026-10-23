package com.donnclab.kinetiframe.studio.camera

import com.donnclab.kinetiframe.studio.drawing.Affine2D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Phase budgets tolerate later lane trims: always leave at least one movement frame. */
object CameraTiming {
    fun progress(shot: CameraShot, frame: Int): Float {
        val span = shot.endFrame - shot.startFrame
        val wait = shot.waitFrames.coerceAtMost(span - 1)
        val settle = shot.settleFrames.coerceAtMost(span - wait - 1)
        val move = span - wait - settle
        val local = frame - shot.startFrame - wait
        if (local < 0) return 0f
        return if (move == 1) 1f else (local.toFloat() / (move - 1)).coerceIn(0f, 1f)
    }
    fun ease(t: Float, easing: CameraEasing): Float = when (easing) {
        CameraEasing.SMOOTH -> t * t * (3f - 2f * t)
        CameraEasing.LINEAR -> t
        CameraEasing.EASE_IN -> t * t
        CameraEasing.EASE_OUT -> 1f - (1f - t) * (1f - t)
    }
}

object SceneCamera {
    /** A subsequent shot must not bake a temporary shake sample into its permanent framing. */
    fun stablePose(shots: List<CameraShot>, frame: Int, fps: Int): CameraPose = evaluate(
        shots.filter {
            it.action != CameraAction.SHAKE && it.action != CameraAction.HANDHELD &&
                it.action != CameraAction.FOLLOW
        },
        frame,
        fps,
    )

    /** Only explicitly held framing survives a range; procedural movement is always temporary.
     * Analytic seeded signals avoid redraw randomness and frame-rate-dependent accumulation. */
    fun evaluate(
        shots: List<CameraShot>,
        frame: Int,
        fps: Int,
        followCenter: ((CameraShot, Int) -> Pair<Float, Float>?)? = null,
    ): CameraPose {
        require(fps > 0)
        var pose = CameraPose()
        for (shot in shots.sortedBy { it.startFrame }) {
            if (frame < shot.startFrame) break
            if (frame >= shot.endFrame) {
                if (shot.holdEnd && shot.action != CameraAction.SHAKE && shot.action != CameraAction.HANDHELD &&
                    shot.action != CameraAction.FOLLOW
                ) {
                    pose = shot.end
                }
                continue
            }
            val t = CameraTiming.progress(shot, frame)
            val eased = CameraTiming.ease(t, shot.easing)
            fun mix(a: Float, b: Float) = a + (b - a) * eased
            if (shot.action == CameraAction.FOLLOW) {
                val span = shot.endFrame - shot.startFrame
                val wait = shot.waitFrames.coerceAtMost(span - 1)
                val settle = shot.settleFrames.coerceAtMost(span - wait - 1)
                val targetFrame = frame.coerceIn(shot.startFrame + wait, shot.endFrame - settle - 1)
                val center = followCenter?.invoke(shot, targetFrame) ?: return shot.start
                return shot.end.copy(
                    x = mix(shot.start.x, (center.first + shot.end.x - .5f).coerceIn(-1f, 2f)),
                    y = mix(shot.start.y, (center.second + shot.end.y - .5f).coerceIn(-1f, 2f)),
                )
            }
            if (shot.action == CameraAction.SHAKE || shot.action == CameraAction.HANDHELD) {
                val time =
                    (
                        frame - shot.startFrame - shot.waitFrames.coerceAtMost(
                            shot.endFrame - shot.startFrame - 1,
                        )
                        ).toDouble() /
                        fps
                val phase = (shot.seed.toLong().and(0xffff) / 65535.0) * 2 * PI
                val speed = shot.frequencyHz?.toDouble() ?: if (shot.action == CameraAction.SHAKE) 13.0 else 1.3
                val envelope = if (t <= 0f || t >= 1f) 0f else sin(PI * t).toFloat() * shot.strength
                val amplitude = if (shot.action == CameraAction.SHAKE) .018f else .006f
                val horizontal = if (shot.shakeDirection == ShakeDirection.VERTICAL) 0f else 1f
                val vertical = if (shot.shakeDirection == ShakeDirection.HORIZONTAL) 0f else 1f
                return pose.copy(
                    x = (
                        pose.x +
                            sin(time * speed * 2 * PI + phase).toFloat() * amplitude * envelope * horizontal
                        ).coerceIn(-1f, 2f),
                    y = (
                        pose.y + sin(
                            time * speed * 1.7 * PI + phase + 1,
                        ).toFloat() * amplitude * envelope * vertical
                        ).coerceIn(-1f, 2f),
                    rotation = (
                        pose.rotation +
                            sin(time * speed * PI + phase).toFloat() * 4f * shot.rotationStrength * envelope
                        ).coerceIn(-180f, 180f),
                )
            }
            val turn = ((shot.end.rotation - shot.start.rotation + 540f) % 360f) - 180f
            val rotation = ((shot.start.rotation + turn * eased + 540f) % 360f) - 180f
            return CameraPose(
                mix(shot.start.x, shot.end.x),
                mix(shot.start.y, shot.end.y),
                mix(shot.start.zoom, shot.end.zoom),
                rotation,
                mix(shot.start.black, shot.end.black),
            )
        }
        return pose
    }

    fun matrix(pose: CameraPose, width: Int, height: Int): Affine2D {
        require(width > 0 && height > 0)
        val angle = -pose.rotation * PI / 180
        val a = (cos(angle) * pose.zoom).toFloat()
        val b = (sin(angle) * pose.zoom).toFloat()
        return Affine2D(
            a,
            b,
            -b,
            a,
            width / 2f - a * pose.x * width + b * pose.y * height,
            height / 2f - b * pose.x * width - a * pose.y * height,
        )
    }

    fun preset(
        id: String,
        action: CameraAction,
        from: Int,
        to: Int,
        base: CameraPose,
        strength: Float = .5f,
    ): CameraShot {
        val end = when (action) {
            CameraAction.PUSH_IN -> base.copy(zoom = (base.zoom * (1f + strength)).coerceAtMost(4f))

            CameraAction.PULL_OUT -> base.copy(zoom = (base.zoom / (1f + strength)).coerceAtLeast(.25f))

            CameraAction.PAN, CameraAction.WHIP_PAN -> base.copy(
                x = (
                    base.x +
                        strength * if (action == CameraAction.WHIP_PAN) .7f else .2f
                    ).coerceAtMost(2f),
            )

            CameraAction.TILT -> base.copy(rotation = (base.rotation + strength * 15f).coerceAtMost(180f))

            CameraAction.FADE_IN -> base.copy(black = 0f)

            CameraAction.FADE_OUT -> base.copy(black = 1f)

            CameraAction.SHAKE, CameraAction.HANDHELD, CameraAction.FOLLOW -> base
        }
        return CameraShot(
            id, action, from, to, if (action == CameraAction.FADE_IN) base.copy(black = 1f) else base,
            end, strength, id.hashCode(),
            easing = if (action ==
                CameraAction.WHIP_PAN
            ) {
                CameraEasing.EASE_IN
            } else {
                CameraEasing.SMOOTH
            },
            settleFrames = if (action == CameraAction.WHIP_PAN) ((to - from) * .6f).toInt() else 0,
        )
    }
}
