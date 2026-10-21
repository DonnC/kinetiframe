// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.rigging

import com.donnclab.kinetiframe.core.actor.artwork.ArtworkVariant
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.actor.artwork.requiredField
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// Cohesive bone records must not replace the flat fields in existing actor and project archives.
@OptIn(ExperimentalSerializationApi::class)
object BoneNodeSerializerImpl : KSerializer<BoneNode> {
    private val variants = MapSerializer(String.serializer(), ArtworkVariant.serializer())
    override val descriptor = buildClassSerialDescriptor("com.kinetiframe.engine.rigging.model.BoneNode") {
        element<String>("id")
        element<String?>("parentId", isOptional = true)
        element<String>("name")
        element<Float>("length")
        element<Transform>("localTransform", isOptional = true)
        element<String?>("textureSlotId", isOptional = true)
        element<TextureBinding?>("textureBinding", isOptional = true)
        element<String?>("ikChainRootId", isOptional = true)
        element<Boolean>("visible", isOptional = true)
        element<Int>("drawOrder", isOptional = true)
        element("artworkVariants", variants.descriptor, isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: BoneNode) {
        val output = encoder.beginStructure(descriptor)
        output.encodeStringElement(descriptor, 0, value.id)
        if (value.parentId != null ||
            output.shouldEncodeElementDefault(descriptor, 1)
        ) {
            output.encodeNullableSerializableElement(descriptor, 1, String.serializer(), value.parentId)
        }
        output.encodeStringElement(descriptor, 2, value.name)
        output.encodeFloatElement(descriptor, 3, value.length)
        if (value.localTransform != Transform() ||
            output.shouldEncodeElementDefault(descriptor, 4)
        ) {
            output.encodeSerializableElement(descriptor, 4, Transform.serializer(), value.localTransform)
        }
        if (value.textureSlotId != null ||
            output.shouldEncodeElementDefault(descriptor, 5)
        ) {
            output.encodeNullableSerializableElement(descriptor, 5, String.serializer(), value.textureSlotId)
        }
        if (value.textureBinding != null ||
            output.shouldEncodeElementDefault(descriptor, 6)
        ) {
            output.encodeNullableSerializableElement(
                descriptor,
                6,
                TextureBinding.serializer(),
                value.textureBinding,
            )
        }
        if (value.ikChainRootId != null ||
            output.shouldEncodeElementDefault(descriptor, 7)
        ) {
            output.encodeNullableSerializableElement(descriptor, 7, String.serializer(), value.ikChainRootId)
        }
        if (!value.visible ||
            output.shouldEncodeElementDefault(descriptor, 8)
        ) {
            output.encodeBooleanElement(descriptor, 8, value.visible)
        }
        if (value.drawOrder != 0 ||
            output.shouldEncodeElementDefault(descriptor, 9)
        ) {
            output.encodeIntElement(descriptor, 9, value.drawOrder)
        }
        if (value.artworkVariants.isNotEmpty() ||
            output.shouldEncodeElementDefault(descriptor, 10)
        ) {
            output.encodeSerializableElement(descriptor, 10, variants, value.artworkVariants)
        }
        output.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): BoneNode {
        var id: String? = null
        var name: String? = null
        var length: Float? = null
        var parentId: String? = null
        var transform = Transform()
        var slot: String? = null
        var binding: TextureBinding? = null
        var chain: String? = null
        var visible = true
        var order = 0
        var alternatives = emptyMap<String, ArtworkVariant>()
        val input = decoder.beginStructure(descriptor)
        fun read(index: Int) {
            when (index) {
                0 -> id = input.decodeStringElement(descriptor, index)

                1 ->
                    parentId =
                        input.decodeNullableSerializableElement(descriptor, index, String.serializer(), parentId)

                2 -> name = input.decodeStringElement(descriptor, index)

                3 -> length = input.decodeFloatElement(descriptor, index)

                4 -> transform = input.decodeSerializableElement(descriptor, index, Transform.serializer(), transform)

                5 -> slot = input.decodeNullableSerializableElement(descriptor, index, String.serializer(), slot)

                6 ->
                    binding =
                        input.decodeNullableSerializableElement(descriptor, index, TextureBinding.serializer(), binding)

                7 -> chain = input.decodeNullableSerializableElement(descriptor, index, String.serializer(), chain)

                8 -> visible = input.decodeBooleanElement(descriptor, index)

                9 -> order = input.decodeIntElement(descriptor, index)

                10 -> alternatives = input.decodeSerializableElement(descriptor, index, variants, alternatives)

                else -> throw BoneDecodeException(BoneDecodeIssue.UNEXPECTED_FIELD)
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
        return BoneNode(
            BoneHierarchy(descriptor.requiredField(id, 0), descriptor.requiredField(name, 2), parentId),
            BoneRest(descriptor.requiredField(length, 3), transform, chain),
            BoneArtwork(slot, binding, alternatives),
            BoneDisplay(visible, order),
        )
    }
}

enum class BoneDecodeIssue { UNEXPECTED_FIELD }
class BoneDecodeException(val issue: BoneDecodeIssue) : SerializationException(issue.name)
