// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.presets

import com.donnclab.kinetiframe.core.actor.artwork.requiredField
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// Grouped runtime controls must retain the flat fields in existing recipe snapshots.
@OptIn(ExperimentalSerializationApi::class)
object PresetRotationChannelSerializerImpl : KSerializer<PresetRotationChannel> {
    private val points = ListSerializer(RotationPoint.serializer())
    private val references = ListSerializer(String.serializer())
    override val descriptor = buildClassSerialDescriptor("com.kinetiframe.engine.rigging.presets.RotationChannel") {
        element<String>("boneId")
        element<Float>("offsetDegrees", isOptional = true)
        element<Float>("amplitudeDegrees", isOptional = true)
        element<Float>("cyclesPerLoop", isOptional = true)
        element<Float>("phaseDegrees", isOptional = true)
        element("points", points.descriptor, isOptional = true)
        element("counterRotationBones", references.descriptor, isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: PresetRotationChannel) {
        val output = encoder.beginStructure(descriptor)
        val controls = value.oscillation
        output.encodeStringElement(descriptor, 0, value.boneId)
        if (controls.offsetDegrees.toBits() != 0f.toBits() ||
            output.shouldEncodeElementDefault(descriptor, 1)
        ) {
            output.encodeFloatElement(descriptor, 1, controls.offsetDegrees)
        }
        if (controls.amplitudeDegrees.toBits() != 0f.toBits() ||
            output.shouldEncodeElementDefault(descriptor, 2)
        ) {
            output.encodeFloatElement(descriptor, 2, controls.amplitudeDegrees)
        }
        if (controls.cyclesPerLoop != 1f ||
            output.shouldEncodeElementDefault(descriptor, 3)
        ) {
            output.encodeFloatElement(descriptor, 3, controls.cyclesPerLoop)
        }
        if (controls.phaseDegrees.toBits() != 0f.toBits() ||
            output.shouldEncodeElementDefault(descriptor, 4)
        ) {
            output.encodeFloatElement(descriptor, 4, controls.phaseDegrees)
        }
        if (value.points.isNotEmpty() ||
            output.shouldEncodeElementDefault(descriptor, 5)
        ) {
            output.encodeSerializableElement(descriptor, 5, points, value.points)
        }
        if (value.counterRotationBones.isNotEmpty() ||
            output.shouldEncodeElementDefault(descriptor, 6)
        ) {
            output.encodeSerializableElement(descriptor, 6, references, value.counterRotationBones)
        }
        output.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): PresetRotationChannel {
        var id: String? = null
        var offset = 0f
        var amplitude = 0f
        var cycles = 1f
        var phase = 0f
        var knots = emptyList<RotationPoint>()
        var parents = emptyList<String>()
        val input = decoder.beginStructure(descriptor)
        fun read(index: Int) {
            when (index) {
                0 -> id = input.decodeStringElement(descriptor, index)
                1 -> offset = input.decodeFloatElement(descriptor, index)
                2 -> amplitude = input.decodeFloatElement(descriptor, index)
                3 -> cycles = input.decodeFloatElement(descriptor, index)
                4 -> phase = input.decodeFloatElement(descriptor, index)
                5 -> knots = input.decodeSerializableElement(descriptor, index, points, knots)
                6 -> parents = input.decodeSerializableElement(descriptor, index, references, parents)
                else -> throw PresetChannelDecodeException(PresetChannelDecodeIssue.UNEXPECTED_FIELD)
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
        return PresetRotationChannel(
            descriptor.requiredField(id, 0),
            PresetOscillation(offset, amplitude, cycles, phase),
            knots,
            parents,
        )
    }
}

enum class PresetChannelDecodeIssue { UNEXPECTED_FIELD }
class PresetChannelDecodeException(val issue: PresetChannelDecodeIssue) : SerializationException(issue.name)
