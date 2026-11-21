// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.artwork

import com.donnclab.kinetiframe.core.geometry.Point2
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// Archives retain flat registration fields even though the Kotlin API groups alignment geometry.
@OptIn(ExperimentalSerializationApi::class)
object TextureBindingSerializerImpl : KSerializer<TextureBinding> {
    override val descriptor = buildClassSerialDescriptor("com.kinetiframe.engine.rigging.model.TextureBinding") {
        element<String>("assetId")
        element<Float>("pivotX")
        element<Float>("pivotY")
        element<Float>("offsetX", isOptional = true)
        element<Float>("offsetY", isOptional = true)
        element<Float>("rotationDegrees", isOptional = true)
        element<Float>("scale", isOptional = true)
        element<ArtworkSkin?>("skin", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: TextureBinding) {
        val output = encoder.beginStructure(descriptor)
        output.encodeStringElement(descriptor, 0, value.assetId)
        output.encodeFloatElement(descriptor, 1, value.pivotX)
        output.encodeFloatElement(descriptor, 2, value.pivotY)
        val values = floatArrayOf(value.offsetX, value.offsetY, value.rotationDegrees, value.scale)
        values.forEachIndexed { index, number ->
            val field = index + 3
            val default = if (field == 6) 1f else 0f
            if (number != default || output.shouldEncodeElementDefault(descriptor, field)) {
                output.encodeFloatElement(descriptor, field, number)
            }
        }
        if (value.skin != null || output.shouldEncodeElementDefault(descriptor, 7)) {
            output.encodeNullableSerializableElement(descriptor, 7, ArtworkSkin.serializer(), value.skin)
        }
        output.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): TextureBinding {
        var assetId: String? = null
        val values = floatArrayOf(0f, 0f, 0f, 0f, 0f, 1f)
        val seen = BooleanArray(2)
        var skin: ArtworkSkin? = null
        val input = decoder.beginStructure(descriptor)
        if (input.decodeSequentially()) {
            assetId = input.decodeStringElement(descriptor, 0)
            values.indices.forEach { values[it] = input.decodeFloatElement(descriptor, it + 1) }
            seen.fill(true)
            skin = input.decodeNullableSerializableElement(descriptor, 7, ArtworkSkin.serializer())
        } else {
            while (true) {
                when (val index = input.decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break

                    0 -> assetId = input.decodeStringElement(descriptor, index)

                    in 1..6 -> {
                        values[index - 1] = input.decodeFloatElement(descriptor, index)
                        if (index <= 2) seen[index - 1] = true
                    }

                    7 ->
                        skin =
                            input.decodeNullableSerializableElement(descriptor, index, ArtworkSkin.serializer(), skin)

                    else -> throw ArtworkDecodeException(ArtworkDecodeIssue.UNEXPECTED_FIELD)
                }
            }
        }
        input.endStructure(descriptor)
        val missing = buildList {
            if (assetId == null) add(descriptor.getElementName(0))
            seen.forEachIndexed { index, present -> if (!present) add(descriptor.getElementName(index + 1)) }
        }
        if (missing.isNotEmpty()) throw MissingFieldException(missing, descriptor.serialName)
        return TextureBinding(
            requireNotNull(assetId),
            Point2(values[0], values[1]),
            ArtworkAlignment(Point2(values[2], values[3]), values[4], values[5]),
            skin,
        )
    }
}
