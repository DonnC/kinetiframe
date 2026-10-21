package com.donnclab.kinetiframe.ui.timeline

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
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.engine.model.AudioClipPlacement
import com.donnclab.kinetiframe.ui.AnimationState
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.TimelineState
import com.donnclab.kinetiframe.ui.components.timeline.TimelineFrameList
import com.donnclab.kinetiframe.ui.components.timeline.audio.ProjectAudioScrubRibbon
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
                    TimelineState(animation = AnimationState(totalFrames = 100)),
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
            assertEquals(0, com.donnclab.kinetiframe.ui.components.timeline.centeredTimelineFrame(list!!))
            scroll()
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(99, com.donnclab.kinetiframe.ui.components.timeline.centeredTimelineFrame(list!!))
        }
    }

    @Test fun draggingFilmstripSelectsCrossedFramesAndPausesPlayback() {
        val intents = mutableListOf<TimelineIntent>()
        compose.setContent {
            KinetiFrameTheme {
                var state by remember { mutableStateOf(TimelineState(animation = AnimationState(totalFrames = 100))) }
                TimelineFrameList(state, { intent ->
                    intents += intent
                    if (intent is TimelineIntent.Playback.SetCurrentFrame) {
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
            assertTrue(intents.contains(TimelineIntent.Playback.Pause))
            assertTrue(intents.filterIsInstance<TimelineIntent.Playback.SetCurrentFrame>().any { it.frame > 0 })
        }
    }

    @Test fun programmaticScrollDoesNotAuditionOrChangeSelection() {
        val intents = mutableListOf<TimelineIntent>()
        var scroll: () -> Unit = {}
        compose.setContent {
            KinetiFrameTheme {
                val list = rememberLazyListState()
                val scope = rememberCoroutineScope()
                scroll = { scope.launch { list.scrollToItem(20) } }
                TimelineFrameList(
                    TimelineState(animation = AnimationState(totalFrames = 100)),
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
