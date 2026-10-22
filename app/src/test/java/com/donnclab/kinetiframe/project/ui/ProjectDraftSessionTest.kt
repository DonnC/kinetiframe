package com.donnclab.kinetiframe.project.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProjectDraftSessionTest {
    @Test fun `preparing a draft keeps its identity and a new lifecycle gets a fresh draft`() {
        val session = ProjectDraftSession()
        val identity = session.state.value.id
        assertFalse(session.state.value.ready)
        session.handleIntent(ProjectDraftSessionIntent.Prepared)
        assertTrue(session.state.value.ready)
        assertEquals(identity, session.state.value.id)
        assertNotEquals(identity, ProjectDraftSession().state.value.id)
    }
}
