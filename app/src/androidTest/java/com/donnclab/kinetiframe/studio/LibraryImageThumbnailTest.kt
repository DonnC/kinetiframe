package com.donnclab.kinetiframe.studio

import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.library.ui.LibraryImageThumbnail
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class LibraryImageThumbnailTest {
    @get:Rule val compose = createComposeRule()

    @Test fun thumbnailRetainsAccessibleNameAndParentSelectionAction() {
        var selected = false
        // A missing local image must not remove the selection target while Coil loads/fails.
        compose.setContent {
            MaterialTheme {
                LibraryImageThumbnail(
                    File("missing-background.png"),
                    "Living room",
                    Modifier.clickable { selected = true },
                )
            }
        }
        compose.onNodeWithContentDescription("Living room preview")
            .assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(selected) }
    }
}
