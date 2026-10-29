package com.donnclab.kinetiframe.studio

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.presentation.studio.EditorAssetMenu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BackgroundVisibilityMenuTest {
    @get:Rule val compose = createComposeRule()

    @Test fun hideAndShowRemainSeparateFromRemove() {
        var visible by mutableStateOf(true)
        var removes = 0
        compose.setContent {
            MaterialTheme {
                EditorAssetMenu(
                    true, {}, true, {}, { removes++ }, null, null,
                    visible, { visible = !visible },
                )
            }
        }
        compose.onNodeWithContentDescription("Hide background").performClick()
        compose.onNodeWithContentDescription("Show background").performClick()
        compose.runOnIdle {
            assertTrue(visible)
            assertEquals(0, removes)
        }
    }

    @Test fun visibilityActionIsAbsentWhenNoBackgroundIsAdded() {
        compose.setContent { MaterialTheme { EditorAssetMenu(true, {}, false, {}, {}, null, null) } }
        compose.onNodeWithContentDescription("Hide background").assertDoesNotExist()
        compose.onNodeWithContentDescription("Show background").assertDoesNotExist()
    }
}
