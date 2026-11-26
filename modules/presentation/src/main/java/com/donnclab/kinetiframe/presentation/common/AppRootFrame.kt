// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

/**
 * A top-level screen (Home, Library, Settings) with its navigation: on a phone the screen shows
 * [AppBottomNavBar] itself; on wider windows a rail down the left side takes its place, which keeps
 * the full height for content and puts the destinations under the thumb on a tablet.
 */
@Composable
fun AppRootFrame(
    active: BottomNavDestination,
    onNavigate: (BottomNavDestination) -> Unit,
    content: @Composable () -> Unit,
) {
    if (windowWidthClass().isCompact) {
        content()
        return
    }
    Row(Modifier.fillMaxSize()) {
        NavigationRail(Modifier.fillMaxHeight().width(Dimens.navRailWidth)) {
            Spacer(Modifier.weight(1f))
            BottomNavDestination.entries.forEach { destination ->
                NavigationRailItem(
                    selected = destination == active,
                    onClick = { onNavigate(destination) },
                    icon = { Icon(destination.icon, null, Modifier.size(Dimens.iconM)) },
                    label = { Text(stringResource(destination.label)) },
                )
            }
            Spacer(Modifier.weight(1f))
        }
        Box(Modifier.weight(1f).fillMaxHeight()) { content() }
    }
}

@Preview(showBackground = true, widthDp = 900, heightDp = 500)
@Composable
private fun AppRootFramePreview() {
    KinetiFrameTheme { AppRootFrame(BottomNavDestination.HOME, {}) {} }
}
