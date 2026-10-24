package com.donnclab.kinetiframe.presentation.common

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import org.junit.Rule
import org.junit.Test

class BrandMarkTest {
    @get:Rule val compose = createComposeRule()

    @Test fun badgeShowsAccessibleBrandAtToolbarSize() {
        compose.setContent { KinetiFrameTheme { BrandBadge(Modifier.size(40.dp)) } }
        compose.onNodeWithContentDescription("KinetiFrame").assertIsDisplayed()
    }

    @Test fun markShowsInDarkTheme() {
        compose.setContent {
            KinetiFrameTheme(darkTheme = true) {
                BrandMark(Modifier.size(48.dp), contentDescription = "Brand")
            }
        }
        compose.onNodeWithContentDescription("Brand").assertIsDisplayed()
    }
}
