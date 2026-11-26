package com.donnclab.kinetiframe.presentation.common

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.presentation.R

/** Top-level bottom-nav destinations. Each screen that shows the bar passes the active one. */
enum class BottomNavDestination(val icon: ImageVector, @StringRes val label: Int) {
    HOME(Icons.Filled.Home, R.string.nav_home),
    LIBRARY(Icons.AutoMirrored.Filled.LibraryBooks, R.string.nav_library),
    SETTINGS(Icons.Filled.Settings, R.string.nav_settings),
}

/**
 * Shared bottom navigation bar: Home / Library / Settings, three equal slots on every top-level
 * screen, so each item stays in the same place when switching. Creating is a separate floating
 * button above the bar, not a slot in it.
 */
@Composable
fun AppBottomNavBar(
    active: BottomNavDestination,
    onNavigate: (BottomNavDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Wider windows show the rail from AppRootFrame instead.
    if (!windowWidthClass().isCompact) return
    Surface(
        modifier = modifier.fillMaxWidth().height(Dimens.bottomNavHeight),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = Dimens.elevationS,
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            BottomNavDestination.entries.forEach { destination ->
                AppBottomNavItem(destination, active, onNavigate, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AppBottomNavItem(
    destination: BottomNavDestination,
    active: BottomNavDestination,
    onNavigate: (BottomNavDestination) -> Unit,
    modifier: Modifier,
) {
    val isActive = destination == active
    val tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier.fillMaxHeight().selectable(selected = isActive, role = Role.Tab, onClick = {
            onNavigate(destination)
        }),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // The label below names the destination, so the icon has no description of its own.
        Icon(destination.icon, null, tint = tint, modifier = Modifier.size(Dimens.iconM))
        Spacer(Modifier.height(Dimens.spaceXs))
        Text(stringResource(destination.label), style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

@Preview(showBackground = true)
@Composable
private fun AppBottomNavBarPreview() {
    KinetiFrameTheme { AppBottomNavBar(active = BottomNavDestination.LIBRARY, onNavigate = {}) }
}
