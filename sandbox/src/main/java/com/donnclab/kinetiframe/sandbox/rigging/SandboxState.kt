// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.sandbox.rigging

import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.core.mvi.ViewIntent
import com.donnclab.kinetiframe.core.mvi.ViewState
import com.donnclab.kinetiframe.engine.math.Fabrik

internal object RigFixture {
    const val WIDTH = 360f
    const val HEIGHT = 300f
    const val MAX_SAMPLES = 300
    const val FRAME_DELAY_MILLIS = 33L
    val joints = listOf(Point2(80f, 160f), Point2(170f, 130f), Point2(240f, 180f))
}
internal data class RigPose(val target: Point2, val joints: List<Point2>)
internal data class CaptureState(val recording: Boolean = false, val targets: List<Point2> = emptyList())
internal data class ReplayState(val playing: Boolean = false, val frame: Int = 0)
internal enum class RigPreviewMode { BONES, SKIN_MESH }
internal data class SandboxState(
    val pose: RigPose = RigPose(RigFixture.joints.last(), Fabrik.solve(RigFixture.joints, RigFixture.joints.last())),
    val capture: CaptureState = CaptureState(),
    val replay: ReplayState = ReplayState(),
    val previewMode: RigPreviewMode = RigPreviewMode.BONES,
) : ViewState

internal sealed interface SandboxIntent : ViewIntent {
    data class Drag(val target: Point2) : SandboxIntent
    data object Reset : SandboxIntent
    data object ToggleCapture : SandboxIntent
    data object ToggleReplay : SandboxIntent
    data object Tick : SandboxIntent
    data object TogglePreview : SandboxIntent
}
