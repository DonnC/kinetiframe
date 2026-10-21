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

// Grouping the public model must not nest fields in existing saved recipe snapshots.
@OptIn(ExperimentalSerializationApi::class)
object PresetRecipeDocumentSerializerImpl : KSerializer<PresetRecipeDocument> {
    private val roles = ListSerializer(String.serializer())
    private val rotations = ListSerializer(PresetRotationChannel.serializer())
    private val roots = ListSerializer(RootMotionChannel.serializer())
    private val views = PresetAuthoredView.serializer()
    override val descriptor = buildClassSerialDescriptor("com.kinetiframe.engine.rigging.presets.ActorMotionPreset") {
        element<Int>("schemaVersion", isOptional = true)
        element<String>("id")
        element<String>("name")
        element<String>("description")
        element<Float>("durationSeconds")
        element("requiredBones", roles.descriptor)
        element("channels", rotations.descriptor)
        element<Boolean>("loop", isOptional = true)
        element("rootMotion", roots.descriptor, isOptional = true)
        element("authoredView", views.descriptor, isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: PresetRecipeDocument) {
        val output = encoder.beginStructure(descriptor)
        val recipe = value.recipe
        val metadata = recipe.metadata
        val channels = recipe.channels
        if (metadata.schemaVersion != 1 ||
            output.shouldEncodeElementDefault(descriptor, 0)
        ) {
            output.encodeIntElement(descriptor, 0, metadata.schemaVersion)
        }
        output.encodeStringElement(descriptor, 1, metadata.id)
        output.encodeStringElement(descriptor, 2, metadata.name)
        output.encodeStringElement(descriptor, 3, value.description)
        output.encodeFloatElement(descriptor, 4, metadata.durationSeconds)
        output.encodeSerializableElement(descriptor, 5, roles, channels.requiredBones)
        output.encodeSerializableElement(descriptor, 6, rotations, channels.rotations)
        if (!recipe.loop ||
            output.shouldEncodeElementDefault(descriptor, 7)
        ) {
            output.encodeBooleanElement(descriptor, 7, recipe.loop)
        }
        if (channels.roots.isNotEmpty() ||
            output.shouldEncodeElementDefault(descriptor, 8)
        ) {
            output.encodeSerializableElement(descriptor, 8, roots, channels.roots)
        }
        if (value.authoredView != PresetAuthoredView.FRONT ||
            output.shouldEncodeElementDefault(descriptor, 9)
        ) {
            output.encodeSerializableElement(descriptor, 9, views, value.authoredView)
        }
        output.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): PresetRecipeDocument {
        var schema = 1
        var id: String? = null
        var name: String? = null
        var description: String? = null
        var duration: Float? = null
        var required: List<String>? = null
        var channels: List<PresetRotationChannel>? = null
        var loop = true
        var rootMotion = emptyList<RootMotionChannel>()
        var view = PresetAuthoredView.FRONT
        val input = decoder.beginStructure(descriptor)
        fun read(index: Int) {
            when (index) {
                0 -> schema = input.decodeIntElement(descriptor, index)
                1 -> id = input.decodeStringElement(descriptor, index)
                2 -> name = input.decodeStringElement(descriptor, index)
                3 -> description = input.decodeStringElement(descriptor, index)
                4 -> duration = input.decodeFloatElement(descriptor, index)
                5 -> required = input.decodeSerializableElement(descriptor, index, roles, required)
                6 -> channels = input.decodeSerializableElement(descriptor, index, rotations, channels)
                7 -> loop = input.decodeBooleanElement(descriptor, index)
                8 -> rootMotion = input.decodeSerializableElement(descriptor, index, roots, rootMotion)
                9 -> view = input.decodeSerializableElement(descriptor, index, views)
                else -> throw PresetRecipeDecodeException(PresetRecipeDecodeIssue.UNEXPECTED_FIELD)
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
        val metadata = PresetRecipeMetadata(
            schema,
            descriptor.requiredField(id, 1),
            descriptor.requiredField(name, 2),
            descriptor.requiredField(duration, 4),
        )
        val content = PresetRecipeChannels(
            descriptor.requiredField(required, 5),
            descriptor.requiredField(channels, 6),
            rootMotion,
        )
        return PresetRecipeDocument(
            PresetRecipe(metadata, content, loop),
            descriptor.requiredField(description, 3),
            view,
        )
    }
}

enum class PresetRecipeDecodeIssue { UNEXPECTED_FIELD }
class PresetRecipeDecodeException(val issue: PresetRecipeDecodeIssue) : SerializationException(issue.name)
