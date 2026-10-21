// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.sandbox.rigging

import com.donnclab.kinetiframe.core.mvi.StateStore
import com.donnclab.kinetiframe.engine.math.Fabrik

/** Lifecycle-neutral MVI controller, not a second solver or private product ViewModel. */
internal class SandboxController {
    private val store = StateStore<SandboxState, Nothing>(SandboxState())
    val state = store.state
    fun close() = store.close()
    fun handleIntent(intent: SandboxIntent) = store.update { state ->
        when (intent) {
            SandboxIntent.Reset -> SandboxState(previewMode = state.previewMode)

            SandboxIntent.TogglePreview -> state.copy(
                previewMode = when (state.previewMode) {
                    RigPreviewMode.BONES -> RigPreviewMode.SKIN_MESH
                    RigPreviewMode.SKIN_MESH -> RigPreviewMode.BONES
                },
            )

            SandboxIntent.ToggleCapture -> state.copy(
                capture = if (state.capture.recording) state.capture.copy(recording = false) else CaptureState(true),
                replay = ReplayState(),
            )

            SandboxIntent.ToggleReplay -> state.copy(
                capture = state.capture.copy(recording = false),
                replay = ReplayState(!state.replay.playing && state.capture.targets.isNotEmpty()),
            )

            is SandboxIntent.Drag -> {
                val targets = if (state.capture.recording) {
                    (state.capture.targets + intent.target).take(RigFixture.MAX_SAMPLES)
                } else {
                    state.capture.targets
                }
                state.copy(
                    pose = RigPose(intent.target, Fabrik.solve(state.pose.joints, intent.target)),
                    capture = CaptureState(state.capture.recording && targets.size < RigFixture.MAX_SAMPLES, targets),
                    replay = ReplayState(),
                )
            }

            SandboxIntent.Tick -> {
                if (!state.replay.playing || state.capture.targets.isEmpty()) {
                    state
                } else {
                    val target = state.capture.targets[state.replay.frame]
                    state.copy(
                        pose = RigPose(target, Fabrik.solve(state.pose.joints, target)),
                        replay = state.replay.copy(frame = (state.replay.frame + 1) % state.capture.targets.size),
                    )
                }
            }
        }
    }
}
