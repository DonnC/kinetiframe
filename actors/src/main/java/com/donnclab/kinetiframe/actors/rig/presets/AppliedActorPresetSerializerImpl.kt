package com.donnclab.kinetiframe.actors.rig.presets

import com.donnclab.kinetiframe.core.actor.presets.AppliedPresetDocument
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object AppliedActorPresetSerializerImpl : KSerializer<AppliedActorPreset> {
    private val delegate = AppliedPresetDocument.serializer()
    override val descriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: AppliedActorPreset) =
        delegate.serialize(encoder, AppliedPresetDocument(value.recipe.document(), value.intensity, value.speed))

    override fun deserialize(decoder: Decoder): AppliedActorPreset {
        val document = delegate.deserialize(decoder)
        return AppliedActorPreset(document.recipe.product(), document.intensity, document.speed)
    }
}
