package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.data.ActorManifest
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplates
import com.donnclab.kinetiframe.core.document.StageDimensions
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ActorTemplateContractTest {
    @Test fun `human template rejects missing structural role`() {
        val graph = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd)
            .copy(bones = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd).bones - "head")
        assertThrows(IllegalArgumentException::class.java) {
            ActorTemplateContract.validate(ActorManifest.HUMAN_TEMPLATE_ID, graph)
        }
    }
}
