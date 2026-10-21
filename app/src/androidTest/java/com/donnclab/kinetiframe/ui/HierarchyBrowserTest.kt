package com.donnclab.kinetiframe.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.donnclab.kinetiframe.data.library.AssetOrganization
import com.donnclab.kinetiframe.data.library.BrowseMode
import com.donnclab.kinetiframe.data.library.BrowserPosition
import com.donnclab.kinetiframe.ui.library.CollectionAssignmentDialog
import com.donnclab.kinetiframe.ui.library.CollectionBrowserControls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HierarchyBrowserTest {
    @get:Rule val compose = createComposeRule()

    @Test fun collectionBrowserHasNoPackCategoriesAndSelectsCollection() {
        var selected: BrowserPosition? = null
        compose.setContent {
            MaterialTheme {
                CollectionBrowserControls(
                    BrowserPosition(BrowseMode.COLLECTIONS),
                    listOf("Loose Signal"),
                    { selected = it },
                )
            }
        }
        compose.onNodeWithText("Any category").assertDoesNotExist()
        compose.onNodeWithText("Loose Signal").performClick()
        compose.runOnIdle { assertEquals("Loose Signal", selected?.collection) }
    }

    @Test fun collectionAssignmentPreservesFavouriteAndIdentityMetadata() {
        var saved: AssetOrganization? = null
        compose.setContent {
            MaterialTheme {
                CollectionAssignmentDialog(AssetOrganization(favourite = true), {}, {
                    saved =
                        it
                })
            }
        }
        compose.onNodeWithText("General Essentials").performTextReplacement("Loose Signal")
        compose.onNodeWithContentDescription("Save collection").performClick()
        compose.runOnIdle {
            assertEquals("Loose Signal", saved?.collection)
            assertTrue(saved!!.favourite)
            assertNull(saved?.displayName)
        }
    }
}
