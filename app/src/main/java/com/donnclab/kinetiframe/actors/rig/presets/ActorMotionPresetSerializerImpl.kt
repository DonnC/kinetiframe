package com.donnclab.kinetiframe.actors.rig.presets

import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeDocumentSerializerImpl
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ActorMotionPresetSerializerImpl : KSerializer<ActorMotionPreset> {
    override val descriptor = PresetRecipeDocumentSerializerImpl.descriptor

    override fun serialize(encoder: Encoder, value: ActorMotionPreset) =
        PresetRecipeDocumentSerializerImpl.serialize(encoder, value.document())

    override fun deserialize(decoder: Decoder): ActorMotionPreset =
        PresetRecipeDocumentSerializerImpl.deserialize(decoder).product()
}
