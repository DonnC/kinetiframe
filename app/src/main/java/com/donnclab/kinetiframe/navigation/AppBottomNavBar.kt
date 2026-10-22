package com.donnclab.kinetiframe.navigation

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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

/** Top-level bottom-nav destinations. Each screen that shows the bar passes the active one. */
enum class BottomNavDestination(val icon: ImageVector, val label: String) {
    HOME(Icons.Filled.Home, "Home"),
    LIBRARY(Icons.AutoMirrored.Filled.LibraryBooks, "Library"),
    SETTINGS(Icons.Filled.Settings, "Settings"),
}

/**
 * Shared bottom navigation bar: Home / Library / [center FAB gap] / Settings. Used by every
 * top-level destination so the chrome stays the same when switching screens.
 *
 * On Home the gap leaves room for the centered create button. Settings then takes the whole right
 * half so the gap stays centered.
 */
@Composable
fun AppBottomNavBar(
    active: BottomNavDestination,
    onNavigate: (BottomNavDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(78.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            AppBottomNavItem(BottomNavDestination.HOME, active, onNavigate, Modifier.weight(1f))
            AppBottomNavItem(BottomNavDestination.LIBRARY, active, onNavigate, Modifier.weight(1f))
            val showsCreateGap = active == BottomNavDestination.HOME
            if (showsCreateGap) Spacer(Modifier.weight(1f))
            AppBottomNavItem(
                BottomNavDestination.SETTINGS,
                active,
                onNavigate,
                Modifier.weight(if (showsCreateGap) 2f else 1f),
            )
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
        Icon(destination.icon, destination.label, tint = tint, modifier = Modifier.size(Dimens.iconM))
        Spacer(Modifier.height(Dimens.spaceXs))
        Text(destination.label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

@Preview(showBackground = true)
@Composable
private fun AppBottomNavBarPreview() {
    KinetiFrameTheme { AppBottomNavBar(active = BottomNavDestination.LIBRARY, onNavigate = {}) }
}
