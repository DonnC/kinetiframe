// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.motion

import com.donnclab.kinetiframe.core.actor.artwork.requiredField
import com.donnclab.kinetiframe.core.geometry.Point2
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// Grouped contact records must retain the original flat fields in saved pose/performance archives.
@OptIn(ExperimentalSerializationApi::class)
object JointContactSerializerImpl : KSerializer<JointContact> {
    override val descriptor = buildClassSerialDescriptor("com.kinetiframe.engine.rigging.model.JointContact") {
        element<Float>("x")
        element<Float>("y")
        element<Int>("bendSide")
        element<String?>("followBoneId", isOptional = true)
        element<Float>("followWorldAngle", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: JointContact) {
        val output = encoder.beginStructure(descriptor)
        output.encodeFloatElement(descriptor, 0, value.x)
        output.encodeFloatElement(descriptor, 1, value.y)
        output.encodeIntElement(descriptor, 2, value.bendSide)
        if (value.followBoneId != null || output.shouldEncodeElementDefault(descriptor, 3)) {
            output.encodeNullableSerializableElement(descriptor, 3, String.serializer(), value.followBoneId)
        }
        if (value.followWorldAngle != 0f || output.shouldEncodeElementDefault(descriptor, 4)) {
            output.encodeFloatElement(descriptor, 4, value.followWorldAngle)
        }
        output.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): JointContact {
        var x: Float? = null
        var y: Float? = null
        var sign: Int? = null
        var follow: String? = null
        var angle = 0f
        val input = decoder.beginStructure(descriptor)
        fun read(index: Int) {
            when (index) {
                0 -> x = input.decodeFloatElement(descriptor, index)
                1 -> y = input.decodeFloatElement(descriptor, index)
                2 -> sign = input.decodeIntElement(descriptor, index)
                3 -> follow = input.decodeNullableSerializableElement(descriptor, index, String.serializer(), follow)
                4 -> angle = input.decodeFloatElement(descriptor, index)
                else -> throw ContactDecodeException(ContactDecodeIssue.UNEXPECTED_FIELD)
            }
        }
        if (input.decodeSequentially()) {
            (0 until descriptor.elementsCount).forEach(::read)
        } else {
            while (true) {
                val index = input.decodeElementIndex(descriptor)
                if (index == CompositeDecoder.DECODE_DONE) break
                read(index)
            }
        }
        input.endStructure(descriptor)
        return JointContact(
            Point2(descriptor.requiredField(x, 0), descriptor.requiredField(y, 1)),
            ContactBend.fromSign(descriptor.requiredField(sign, 2)),
            ContactFollow(follow, angle),
        )
    }
}

enum class ContactDecodeIssue { UNEXPECTED_FIELD }
class ContactDecodeException(val issue: ContactDecodeIssue) : SerializationException(issue.name)
