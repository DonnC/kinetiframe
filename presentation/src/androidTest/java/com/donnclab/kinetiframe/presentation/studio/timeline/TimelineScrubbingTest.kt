package com.donnclab.kinetiframe.presentation.studio.timeline

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.height
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.presentation.studio.timeline.audio.ProjectAudioScrubRibbon
import com.donnclab.kinetiframe.studio.AnimationState
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TimelineScrubbingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun firstAndLastFrameCanBothReachTheCenterArrow() {
        var list: androidx.compose.foundation.lazy.LazyListState? = null
        var scroll: () -> Unit = {}
        compose.setContent {
            KinetiFrameTheme {
                val state = rememberLazyListState()
                list = state
                val scope = rememberCoroutineScope()
                scroll = { scope.launch { state.scrollToItem(99) } }
                TimelineFrameList(
                    StudioState(animation = AnimationState(totalFrames = 100)),
                    {},
                    state,
                    emptyMap(),
                    {},
                    {},
                    Modifier.width(240.dp),
                )
            }
        }
        compose.onNodeWithContentDescription("Center frame playhead").assertExists()
        compose.runOnIdle {
            assertEquals(0, com.donnclab.kinetiframe.presentation.studio.timeline.centeredTimelineFrame(list!!))
            scroll()
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(99, com.donnclab.kinetiframe.presentation.studio.timeline.centeredTimelineFrame(list!!))
        }
    }

    @Test fun draggingFilmstripSelectsCrossedFramesAndPausesPlayback() {
        val intents = mutableListOf<StudioIntent>()
        compose.setContent {
            KinetiFrameTheme {
                var state by remember { mutableStateOf(StudioState(animation = AnimationState(totalFrames = 100))) }
                TimelineFrameList(state, { intent ->
                    intents += intent
                    if (intent is StudioIntent.Playback.SetCurrentFrame) {
                        state = state.copy(animation = state.animation.copy(currentFrame = intent.frame))
                    }
                }, rememberLazyListState(), emptyMap(), {}, {}, Modifier.width(240.dp).testTag("filmstrip"))
            }
        }
        compose.onNodeWithTag("filmstrip").performTouchInput {
            swipe(Offset(width * .7f, height / 2f), Offset(width * .15f, height / 2f), 600)
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(intents.contains(StudioIntent.Playback.Pause))
            assertTrue(intents.filterIsInstance<StudioIntent.Playback.SetCurrentFrame>().any { it.frame > 0 })
        }
    }

    @Test fun programmaticScrollDoesNotAuditionOrChangeSelection() {
        val intents = mutableListOf<StudioIntent>()
        var scroll: () -> Unit = {}
        compose.setContent {
            KinetiFrameTheme {
                val list = rememberLazyListState()
                val scope = rememberCoroutineScope()
                scroll = { scope.launch { list.scrollToItem(20) } }
                TimelineFrameList(
                    StudioState(animation = AnimationState(totalFrames = 100)),
                    { intents += it },
                    list,
                    emptyMap(),
                    {},
                    {},
                    Modifier.width(240.dp),
                )
            }
        }
        compose.runOnIdle { scroll() }
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(intents.isEmpty()) }
    }

    @Test fun audioWaveformIsAnExplicitBidirectionalFrameSeekSurface() {
        val selected = mutableListOf<Int>()
        compose.setContent {
            KinetiFrameTheme {
                ProjectAudioScrubRibbon(
                    listOf(
                        AudioClipPlacement(
                            "voice",
                            "asset",
                            0,
                            "Voice",
                            0,
                            30,
                            waveformPeaks = listOf(.2f, .8f),
                        ),
                    ),
                    emptySet(),
                    0,
                    30,
                    rememberLazyListState(),
                    { selected += it },
                    Modifier.width(240.dp),
                )
            }
        }
        compose.onNodeWithContentDescription("Project audio scrubber").performTouchInput {
            swipe(Offset(width * .8f, height / 2f), Offset(width * .2f, height / 2f), 600)
        }
        compose.runOnIdle {
            assertTrue(selected.isNotEmpty())
            assertTrue(selected.first() > selected.last())
        }
    }
}
