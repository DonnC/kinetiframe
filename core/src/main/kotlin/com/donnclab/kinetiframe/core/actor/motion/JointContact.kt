// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.motion

import com.donnclab.kinetiframe.core.geometry.Point2
import kotlinx.serialization.Serializable

sealed class ContactBend(val sign: Int) {
    data object Negative : ContactBend(-1)
    data object Positive : ContactBend(1)
    data class Unrecognized(val rawSign: Int) : ContactBend(rawSign)

    companion object {
        fun fromSign(sign: Int): ContactBend = when (sign) {
            -1 -> Negative
            1 -> Positive
            else -> Unrecognized(sign)
        }
    }
}

data class ContactFollow(val boneId: String? = null, val worldAngle: Float = 0f) {
    companion object {
        val None = ContactFollow()
    }
}

@Serializable(with = JointContactSerializerImpl::class)
data class JointContact(val position: Point2, val bend: ContactBend, val follow: ContactFollow = ContactFollow.None) {
    constructor(x: Float, y: Float, bendSide: Int, followBoneId: String? = null) :
        this(Point2(x, y), ContactBend.fromSign(bendSide), ContactFollow(followBoneId))

    val x: Float get() = position.x
    val y: Float get() = position.y
    val bendSide: Int get() = bend.sign
    val followBoneId: String? get() = follow.boneId
    val followWorldAngle: Float get() = follow.worldAngle

    fun withBendSide(sign: Int): JointContact = copy(bend = ContactBend.fromSign(sign))
}
