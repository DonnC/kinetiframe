// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.presets

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("com.kinetiframe.engine.rigging.presets.RotationPoint")
data class RotationPoint(val progress: Float, val degrees: Float)

@Serializable
@SerialName("com.kinetiframe.engine.rigging.presets.RootMotionPoint")
data class RootMotionPoint(val progress: Float, val x: Float = 0f, val y: Float = 0f)

@Serializable
@SerialName("com.kinetiframe.engine.rigging.presets.RootMotionChannel")
data class RootMotionChannel(val boneId: String, val points: List<RootMotionPoint>)
