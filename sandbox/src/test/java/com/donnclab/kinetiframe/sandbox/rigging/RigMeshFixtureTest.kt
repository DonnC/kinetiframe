// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.sandbox.rigging

import com.donnclab.kinetiframe.core.geometry.Point2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RigMeshFixtureTest {
    @Test fun `procedural mesh uses shared engine and remains finite while posing`() {
        val original = RigMeshFixture.deform(RigFixture.joints)
        assertEquals((RigMeshFixture.COLUMNS + 1) * (RigMeshFixture.ROWS + 1) * 2, original.size)
        assertTrue(original.all(Float::isFinite))
        val controller = SandboxController()
        controller.handleIntent(SandboxIntent.TogglePreview)
        controller.handleIntent(SandboxIntent.Drag(Point2(100f, 50f)))
        val changed = RigMeshFixture.deform(controller.state.value.pose.joints)
        assertTrue(changed.all(Float::isFinite))
        assertFalse(original.contentEquals(changed))
        controller.handleIntent(SandboxIntent.Reset)
        assertEquals(RigPreviewMode.SKIN_MESH, controller.state.value.previewMode)
        controller.close()
    }
}
