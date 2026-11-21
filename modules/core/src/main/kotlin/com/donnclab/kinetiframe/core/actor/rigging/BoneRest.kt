// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.rigging

data class BoneRest(val length: Float, val localTransform: Transform = Transform(), val ikChainRootId: String? = null)
