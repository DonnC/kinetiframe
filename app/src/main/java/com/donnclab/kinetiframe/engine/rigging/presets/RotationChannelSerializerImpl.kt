package com.donnclab.kinetiframe.engine.rigging.presets

import com.donnclab.kinetiframe.core.actor.presets.PresetRotationChannelSerializerImpl
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object RotationChannelSerializerImpl : KSerializer<RotationChannel> {
    override val descriptor = PresetRotationChannelSerializerImpl.descriptor

    override fun serialize(encoder: Encoder, value: RotationChannel) =
        PresetRotationChannelSerializerImpl.serialize(encoder, value.neutral())

    override fun deserialize(decoder: Decoder): RotationChannel =
        PresetRotationChannelSerializerImpl.deserialize(decoder).product()
}
