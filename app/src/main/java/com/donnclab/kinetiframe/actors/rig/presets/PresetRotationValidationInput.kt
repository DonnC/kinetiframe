package com.donnclab.kinetiframe.actors.rig.presets

import com.donnclab.kinetiframe.core.actor.presets.PresetRotationChannel

// Reject metadata/count failures before allocating adapted channel records.
internal class PresetRotationValidationInput(private val source: List<RotationChannel>) :
    AbstractList<PresetRotationChannel>() {
    private val adapted by lazy { source.map { it.neutral() } }

    override val size: Int get() = source.size
    override fun get(index: Int): PresetRotationChannel = adapted[index]
}
