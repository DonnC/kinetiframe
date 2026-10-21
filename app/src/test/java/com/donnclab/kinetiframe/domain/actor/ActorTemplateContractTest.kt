package com.donnclab.kinetiframe.domain.actor

import com.donnclab.kinetiframe.data.actor.ActorManifest
import com.donnclab.kinetiframe.engine.model.StageDimensions
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplate
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplates
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
