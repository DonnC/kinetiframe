package com.donnclab.kinetiframe.presentation.studio.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.cancel
import androidx.compose.ui.test.center
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TimelineAssetClipTest {
    @get:Rule val compose = createComposeRule()

    @Test fun selectingAClipDoesNotCommitTimingChanges() {
        var selected = 0
        var writes = 0
        compose.setContent {
            MaterialTheme {
                Box {
                    TimelineAssetClip(
                        "voice",
                        "Voice",
                        0,
                        8,
                        false,
                        onSelect = { selected++ },
                        onMove = { _, _ -> writes++ },
                        onTrim = { _, _ -> writes++ },
                    )
                }
            }
        }
        compose.onNodeWithText("Voice").performClick()
        compose.runOnIdle {
            assertEquals(1, selected)
            assertEquals(0, writes)
        }
    }

    @Test fun cancellingATrimDoesNotWriteTheDocument() {
        var writes = 0
        compose.setContent {
            MaterialTheme {
                Box {
                    TimelineAssetClip(
                        "voice",
                        "Voice",
                        0,
                        8,
                        true,
                        onSelect = {},
                        onMove = { _, _ -> writes++ },
                        onTrim = { _, _ -> writes++ },
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("Trim start of Voice").performTouchInput {
            down(center)
            moveBy(Offset(80f, 0f))
            cancel()
        }
        compose.runOnIdle { assertEquals(0, writes) }
    }

    @Test fun lockedClipsDoNotExposeTrimGestures() {
        compose.setContent {
            MaterialTheme {
                Box {
                    TimelineAssetClip(
                        "voice", "Voice", 0, 8, true, locked = true,
                        onSelect = {
                        }, onMove = { _, _ ->
                            error("Locked clip moved")
                        }, onTrim = { _, _ -> error("Locked clip trimmed") },
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("Trim start of Voice").assertDoesNotExist()
        compose.onNodeWithContentDescription("Trim end of Voice").assertDoesNotExist()
    }
}
