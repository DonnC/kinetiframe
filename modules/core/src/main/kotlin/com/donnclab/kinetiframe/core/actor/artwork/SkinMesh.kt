// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.artwork

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("com.kinetiframe.engine.rigging.model.SkinBone")
data class SkinBone(val boneId: String, val bindWorld: SkinAffine)

@Serializable
@SerialName("com.kinetiframe.engine.rigging.model.SkinVertex")
data class SkinVertex(val x: Float, val y: Float, val weights: List<Float>)

data class SkinMesh(val bones: List<SkinBone>, val vertices: List<SkinVertex>)
