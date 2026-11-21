// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.motion

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("com.kinetiframe.data.actor.PerformancePlayback")
enum class PerformancePlayback { LOOP, ONE_SHOT, END_HOLD }

@Serializable
@SerialName("com.kinetiframe.data.actor.CurveInterpolation")
enum class CurveInterpolation { LINEAR, STEP, SMOOTH, EASE_IN_OUT }
