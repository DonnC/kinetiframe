// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.presets

import kotlinx.serialization.Serializable

data class PresetOscillation(
    val offsetDegrees: Float = 0f,
    val amplitudeDegrees: Float = 0f,
    val cyclesPerLoop: Float = 1f,
    val phaseDegrees: Float = 0f,
)

@Serializable(with = PresetRotationChannelSerializerImpl::class)
data class PresetRotationChannel(
    val boneId: String,
    val oscillation: PresetOscillation = PresetOscillation(),
    val points: List<RotationPoint> = emptyList(),
    val counterRotationBones: List<String> = emptyList(),
)
