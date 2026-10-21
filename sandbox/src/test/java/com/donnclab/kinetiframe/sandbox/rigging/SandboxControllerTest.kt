// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.sandbox.rigging

import com.donnclab.kinetiframe.core.geometry.Point2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class SandboxControllerTest {
    @Test fun `capture replay reset execute the shared solver and preserve root`() {
        val controller = SandboxController()
        controller.handleIntent(SandboxIntent.ToggleCapture)
        val target = Point2(180f, 220f)
        controller.handleIntent(SandboxIntent.Drag(target))
        controller.handleIntent(SandboxIntent.ToggleCapture)
        assertEquals(listOf(target), controller.state.value.capture.targets)
        assertEquals(RigFixture.joints.first(), controller.state.value.pose.joints.first())
        controller.handleIntent(SandboxIntent.ToggleReplay)
        controller.handleIntent(SandboxIntent.Tick)
        assertEquals(target, controller.state.value.pose.target)
        controller.handleIntent(SandboxIntent.Reset)
        assertEquals(SandboxState(), controller.state.value)
        controller.close()
    }

    @Test fun `capture remains bounded and empty replay cannot start`() {
        val controller = SandboxController()
        controller.handleIntent(SandboxIntent.ToggleReplay)
        assertFalse(controller.state.value.replay.playing)
        controller.handleIntent(SandboxIntent.ToggleCapture)
        repeat(RigFixture.MAX_SAMPLES + 10) { controller.handleIntent(SandboxIntent.Drag(Point2(170f, 230f))) }
        assertEquals(RigFixture.MAX_SAMPLES, controller.state.value.capture.targets.size)
        assertFalse(controller.state.value.capture.recording)
        controller.close()
    }
}
