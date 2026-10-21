// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.rigging

import com.donnclab.kinetiframe.core.geometry.Point2
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// Native archives keep the original flat TRS fields rather than the grouped Kotlin properties.
@OptIn(ExperimentalSerializationApi::class)
object TransformSerializerImpl : KSerializer<Transform> {
    private val fields = listOf("x", "y", "rotationDegrees", "scaleX", "scaleY")
    override val descriptor = buildClassSerialDescriptor("com.kinetiframe.engine.rigging.model.Transform") {
        fields.forEach { element<Float>(it, isOptional = true) }
    }

    override fun serialize(encoder: Encoder, value: Transform) {
        val output = encoder.beginStructure(descriptor)
        val values = floatArrayOf(value.x, value.y, value.rotationDegrees, value.scaleX, value.scaleY)
        values.forEachIndexed { index, number ->
            if (number != default(index) || output.shouldEncodeElementDefault(descriptor, index)) {
                output.encodeFloatElement(descriptor, index, number)
            }
        }
        output.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): Transform {
        val values = FloatArray(fields.size, ::default)
        val input = decoder.beginStructure(descriptor)
        if (input.decodeSequentially()) {
            fields.indices.forEach { values[it] = input.decodeFloatElement(descriptor, it) }
        } else {
            while (true) {
                val index = input.decodeElementIndex(descriptor)
                if (index == CompositeDecoder.DECODE_DONE) break
                if (index !in fields.indices) throw TransformDecodeException(TransformDecodeIssue.UNEXPECTED_FIELD)
                values[index] = input.decodeFloatElement(descriptor, index)
            }
        }
        input.endStructure(descriptor)
        return Transform(Point2(values[0], values[1]), values[2], Point2(values[3], values[4]))
    }

    private fun default(index: Int): Float = if (index < 3) 0f else 1f
}

enum class TransformDecodeIssue { UNEXPECTED_FIELD }
class TransformDecodeException(val issue: TransformDecodeIssue) : SerializationException(issue.name)
