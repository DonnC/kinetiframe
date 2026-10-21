// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.artwork

import com.donnclab.kinetiframe.core.geometry.Point2
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// Grouped geometry must not replace the six flat coefficients already stored in actor archives.
@OptIn(ExperimentalSerializationApi::class)
object SkinAffineSerializer : KSerializer<SkinAffine> {
    private val names = listOf("a", "b", "c", "d", "tx", "ty")
    override val descriptor = buildClassSerialDescriptor("com.kinetiframe.engine.rigging.model.SkinAffine") {
        names.forEach { element<Float>(it) }
    }

    override fun serialize(encoder: Encoder, value: SkinAffine) {
        val output = encoder.beginStructure(descriptor)
        output.encodeFloatElement(descriptor, 0, value.a)
        output.encodeFloatElement(descriptor, 1, value.b)
        output.encodeFloatElement(descriptor, 2, value.c)
        output.encodeFloatElement(descriptor, 3, value.d)
        output.encodeFloatElement(descriptor, 4, value.tx)
        output.encodeFloatElement(descriptor, 5, value.ty)
        output.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): SkinAffine {
        val values = FloatArray(names.size)
        val seen = BooleanArray(names.size)
        val input = decoder.beginStructure(descriptor)
        if (input.decodeSequentially()) {
            names.indices.forEach {
                values[it] = input.decodeFloatElement(descriptor, it)
                seen[it] = true
            }
        } else {
            while (true) {
                val index = input.decodeElementIndex(descriptor)
                if (index == CompositeDecoder.DECODE_DONE) break
                if (index !in names.indices) throw ArtworkDecodeException(ArtworkDecodeIssue.UNEXPECTED_FIELD)
                values[index] = input.decodeFloatElement(descriptor, index)
                seen[index] = true
            }
        }
        input.endStructure(descriptor)
        val missing = names.filterIndexed { index, _ -> !seen[index] }
        if (missing.isNotEmpty()) throw MissingFieldException(missing, descriptor.serialName)
        return SkinAffine(Point2(values[0], values[1]), Point2(values[2], values[3]), Point2(values[4], values[5]))
    }
}

enum class ArtworkDecodeIssue { UNEXPECTED_FIELD }
class ArtworkDecodeException(val issue: ArtworkDecodeIssue) : SerializationException(issue.name)
