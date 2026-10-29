package com.donnclab.kinetiframe.presentation.library.packs

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.library.model.BrowseMode
import com.donnclab.kinetiframe.library.model.BrowserPosition

@Composable
fun PackBrowserControls(
    position: BrowserPosition,
    collections: List<String>,
    onChange: (BrowserPosition) -> Unit,
    showCategories: Boolean = true,
) {
    com.donnclab.kinetiframe.presentation.library.CollectionBrowserControls(
        position,
        collections,
        onChange,
        showCategories,
    )
}

@Preview(showBackground = true)
@Composable
private fun PackBrowserControlsPreview() {
    KinetiFrameTheme {
        PackBrowserControls(BrowserPosition(BrowseMode.COLLECTIONS), listOf("Loose Signal", "General Essentials"), {})
    }
}
