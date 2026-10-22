package com.donnclab.kinetiframe.domain.manager

import com.donnclab.kinetiframe.models.Layer
import com.donnclab.kinetiframe.models.LayerType
import com.donnclab.kinetiframe.ui.LayerState
import com.donnclab.kinetiframe.ui.StudioState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LayerManagerTest {
    @Test
    fun `adding puppet layer creates a definition and scene instance`() {
        var state = StudioState(
            project = com.donnclab.kinetiframe.engine.model.ProjectDocument(
                layers = listOf(Layer("Main", id = "main")),
            ),
            layers = LayerState(
                layers = listOf(Layer("Main", id = "main")),
                frameLayers = mapOf(0 to listOf(Layer("Main", id = "main"))),
            ),
        )
        val manager = LayerManager(
            updateState = { reducer -> state = reducer(state) },
            refreshComposite = {},
        )

        manager.addLayer(LayerType.PUPPET)

        val puppetLayer = state.layers.layers.last()
        val instanceId = requireNotNull(puppetLayer.puppetInstanceId)
        val instance = requireNotNull(state.project.puppetInstances[instanceId])

        assertEquals(LayerType.PUPPET, puppetLayer.type)
        assertNotNull(state.project.puppetDefinitions[instance.definitionId])
        assertTrue(state.project.layers.any { it.id == puppetLayer.id })
    }
}
