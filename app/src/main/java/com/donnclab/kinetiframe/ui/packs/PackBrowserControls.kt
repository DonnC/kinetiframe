package com.donnclab.kinetiframe.ui.packs

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.data.library.BrowseMode
import com.donnclab.kinetiframe.data.library.BrowserPosition
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

@Composable
fun PackBrowserControls(
    position: BrowserPosition,
    collections: List<String>,
    onChange: (BrowserPosition) -> Unit,
    showCategories: Boolean = true,
) {
    com.donnclab.kinetiframe.ui.library.CollectionBrowserControls(position, collections, onChange, showCategories)
}

@Preview(showBackground = true)
@Composable
private fun PackBrowserControlsPreview() {
    KinetiFrameTheme {
        PackBrowserControls(BrowserPosition(BrowseMode.COLLECTIONS), listOf("Loose Signal", "General Essentials"), {})
    }
}
