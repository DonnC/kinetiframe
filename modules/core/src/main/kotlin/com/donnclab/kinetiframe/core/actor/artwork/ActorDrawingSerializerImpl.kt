// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.artwork

import com.donnclab.kinetiframe.core.geometry.Point2
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// Grouped authoring inputs must not change the flat fields of existing drawing archives.
@OptIn(ExperimentalSerializationApi::class)
object ActorDrawingSerializerImpl : KSerializer<ActorDrawing> {
    private val regionList = ListSerializer(ActorDrawingRegion.serializer())
    private val partSet = SetSerializer(String.serializer())
    override val descriptor = buildClassSerialDescriptor("com.kinetiframe.engine.rigging.model.ActorDrawing") {
        element<String>("inkAssetId")
        element<String>("colourAssetId")
        element<Int>("width")
        element<Int>("height")
        element<Float>("originX")
        element<Float>("originY")
        element<Float>("actorWidth")
        element<Float>("actorHeight")
        element("skin", ArtworkSkin.serializer().descriptor)
        element<Boolean>("enabled", isOptional = true)
        element("regions", regionList.descriptor, isOptional = true)
        element("overlayParts", partSet.descriptor, isOptional = true)
        element("underlayParts", partSet.descriptor, isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: ActorDrawing) {
        val output = encoder.beginStructure(descriptor)
        output.encodeStringElement(descriptor, 0, value.inkAssetId)
        output.encodeStringElement(descriptor, 1, value.colourAssetId)
        output.encodeIntElement(descriptor, 2, value.width)
        output.encodeIntElement(descriptor, 3, value.height)
        output.encodeFloatElement(descriptor, 4, value.originX)
        output.encodeFloatElement(descriptor, 5, value.originY)
        output.encodeFloatElement(descriptor, 6, value.actorWidth)
        output.encodeFloatElement(descriptor, 7, value.actorHeight)
        output.encodeSerializableElement(descriptor, 8, ArtworkSkin.serializer(), value.skin)
        if (!value.enabled ||
            output.shouldEncodeElementDefault(descriptor, 9)
        ) {
            output.encodeBooleanElement(descriptor, 9, value.enabled)
        }
        if (value.regions.isNotEmpty() ||
            output.shouldEncodeElementDefault(descriptor, 10)
        ) {
            output.encodeSerializableElement(descriptor, 10, regionList, value.regions)
        }
        if (value.overlayParts.isNotEmpty() ||
            output.shouldEncodeElementDefault(descriptor, 11)
        ) {
            output.encodeSerializableElement(descriptor, 11, partSet, value.overlayParts)
        }
        if (value.underlayParts.isNotEmpty() ||
            output.shouldEncodeElementDefault(descriptor, 12)
        ) {
            output.encodeSerializableElement(descriptor, 12, partSet, value.underlayParts)
        }
        output.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): ActorDrawing {
        val assets = arrayOfNulls<String>(2)
        val size = arrayOfNulls<Int>(2)
        val placement = arrayOfNulls<Float>(4)
        var skin: ArtworkSkin? = null
        var enabled = true
        var regions = emptyList<ActorDrawingRegion>()
        var overlays = emptySet<String>()
        var underlays = emptySet<String>()
        val input = decoder.beginStructure(descriptor)
        fun read(index: Int) {
            when (index) {
                in 0..1 -> assets[index] = input.decodeStringElement(descriptor, index)
                in 2..3 -> size[index - 2] = input.decodeIntElement(descriptor, index)
                in 4..7 -> placement[index - 4] = input.decodeFloatElement(descriptor, index)
                8 -> skin = input.decodeSerializableElement(descriptor, index, ArtworkSkin.serializer(), skin)
                9 -> enabled = input.decodeBooleanElement(descriptor, index)
                10 -> regions = input.decodeSerializableElement(descriptor, index, regionList, regions)
                11 -> overlays = input.decodeSerializableElement(descriptor, index, partSet, overlays)
                12 -> underlays = input.decodeSerializableElement(descriptor, index, partSet, underlays)
                else -> throw ArtworkDecodeException(ArtworkDecodeIssue.UNEXPECTED_FIELD)
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
        return ActorDrawing(
            DrawingSource(
                descriptor.requiredField(assets[0], 0),
                descriptor.requiredField(assets[1], 1),
                SkinSourceSize(descriptor.requiredField(size[0], 2), descriptor.requiredField(size[1], 3)),
            ),
            DrawingPlacement(
                Point2(descriptor.requiredField(placement[0], 4), descriptor.requiredField(placement[1], 5)),
                DrawingExtent(descriptor.requiredField(placement[2], 6), descriptor.requiredField(placement[3], 7)),
            ),
            DrawingBinding(descriptor.requiredField(skin, 8), regions),
            DrawingComposition(enabled, overlays, underlays),
        )
    }
}
