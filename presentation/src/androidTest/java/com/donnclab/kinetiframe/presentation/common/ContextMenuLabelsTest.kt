package com.donnclab.kinetiframe.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class ContextMenuLabelsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun captionsAndActions() {
        var clicked = false
        compose.setContent {
            CompositionLocalProvider(LocalShowMenuLabels provides true) {
                IconContextMenu(true, {
                }, listOf(ContextIconAction(Icons.Filled.ContentCopy, "Copy", { clicked = true })))
            }
        }
        compose.onNodeWithText("Copy").assertExists()
        compose.onNodeWithContentDescription("Copy").performClick()
        compose.runOnIdle { check(clicked) }
    }

    @Test fun hiddenCaptionsPreserveAccessibleIcons() {
        compose.setContent {
            CompositionLocalProvider(LocalShowMenuLabels provides false) {
                IconContextMenu(true, {}, listOf(ContextIconAction(Icons.Filled.ContentCopy, "Copy", {})))
            }
        }
        compose.onNodeWithText("Copy").assertDoesNotExist()
        compose.onNodeWithContentDescription("Copy").assertExists()
    }
}
