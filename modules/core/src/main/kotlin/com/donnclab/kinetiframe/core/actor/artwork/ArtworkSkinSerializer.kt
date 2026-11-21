// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.artwork

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(ExperimentalSerializationApi::class)
object ArtworkSkinSerializer : KSerializer<ArtworkSkin> {
    private val boneList = ListSerializer(SkinBone.serializer())
    private val vertexList = ListSerializer(SkinVertex.serializer())
    override val descriptor = buildClassSerialDescriptor("com.kinetiframe.engine.rigging.model.ArtworkSkin") {
        element<Int>("sourceWidth")
        element<Int>("sourceHeight")
        element<Int>("columns")
        element<Int>("rows")
        element("bones", boneList.descriptor)
        element("vertices", vertexList.descriptor)
        element<Boolean>("attachmentBlend", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: ArtworkSkin) {
        val output = encoder.beginStructure(descriptor)
        output.encodeIntElement(descriptor, 0, value.sourceWidth)
        output.encodeIntElement(descriptor, 1, value.sourceHeight)
        output.encodeIntElement(descriptor, 2, value.columns)
        output.encodeIntElement(descriptor, 3, value.rows)
        output.encodeSerializableElement(descriptor, 4, boneList, value.bones)
        output.encodeSerializableElement(descriptor, 5, vertexList, value.vertices)
        if (value.attachmentBlend || output.shouldEncodeElementDefault(descriptor, 6)) {
            output.encodeBooleanElement(descriptor, 6, value.attachmentBlend)
        }
        output.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): ArtworkSkin {
        val dimensions = arrayOfNulls<Int>(4)
        var bones: List<SkinBone>? = null
        var vertices: List<SkinVertex>? = null
        var attachmentBlend = false
        val input = decoder.beginStructure(descriptor)
        if (input.decodeSequentially()) {
            dimensions.indices.forEach { dimensions[it] = input.decodeIntElement(descriptor, it) }
            bones = input.decodeSerializableElement(descriptor, 4, boneList)
            vertices = input.decodeSerializableElement(descriptor, 5, vertexList)
            attachmentBlend = input.decodeBooleanElement(descriptor, 6)
        } else {
            while (true) {
                when (val index = input.decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break
                    in dimensions.indices -> dimensions[index] = input.decodeIntElement(descriptor, index)
                    4 -> bones = input.decodeSerializableElement(descriptor, index, boneList, bones)
                    5 -> vertices = input.decodeSerializableElement(descriptor, index, vertexList, vertices)
                    6 -> attachmentBlend = input.decodeBooleanElement(descriptor, index)
                    else -> throw ArtworkDecodeException(ArtworkDecodeIssue.UNEXPECTED_FIELD)
                }
            }
        }
        input.endStructure(descriptor)
        return ArtworkSkin(
            SkinSourceSize(descriptor.requiredField(dimensions[0], 0), descriptor.requiredField(dimensions[1], 1)),
            SkinGrid(descriptor.requiredField(dimensions[2], 2), descriptor.requiredField(dimensions[3], 3)),
            SkinMesh(descriptor.requiredField(bones, 4), descriptor.requiredField(vertices, 5)),
            attachmentBlend,
        )
    }
}
