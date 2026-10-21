// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.sandbox.rigging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

@Composable
internal fun RiggingRoute() {
    val controller = remember { SandboxController() }
    val state by controller.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(controller) { onDispose { controller.close() } }
    LaunchedEffect(controller, state.replay.playing, lifecycle) {
        if (state.replay.playing) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(RigFixture.FRAME_DELAY_MILLIS)
                    controller.handleIntent(SandboxIntent.Tick)
                }
            }
        }
    }
    RiggingScreen(state, controller::handleIntent)
}
