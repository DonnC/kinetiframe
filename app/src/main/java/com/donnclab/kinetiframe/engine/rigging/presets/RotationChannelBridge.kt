package com.donnclab.kinetiframe.engine.rigging.presets

import com.donnclab.kinetiframe.core.actor.presets.PresetOscillation
import com.donnclab.kinetiframe.core.actor.presets.PresetRotationChannel

internal fun RotationChannel.neutral(): PresetRotationChannel = PresetRotationChannel(
    boneId,
    PresetOscillation(offsetDegrees, amplitudeDegrees, cyclesPerLoop, phaseDegrees),
    points,
    counterRotationBones,
)

internal fun PresetRotationChannel.product(): RotationChannel = RotationChannel(
    boneId,
    oscillation.offsetDegrees,
    oscillation.amplitudeDegrees,
    oscillation.cyclesPerLoop,
    oscillation.phaseDegrees,
    points,
    counterRotationBones,
)
