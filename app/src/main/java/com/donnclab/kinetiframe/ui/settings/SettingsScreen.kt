package com.donnclab.kinetiframe.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.data.settings.AppPreferences
import com.donnclab.kinetiframe.data.settings.AppTheme
import com.donnclab.kinetiframe.data.settings.StudioOrientation
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.ui.components.common.ContextIconAction
import com.donnclab.kinetiframe.ui.components.common.ContextIconButton

val LocalAppPreferences = staticCompositionLocalOf { AppPreferences() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
    onNavigate: ((com.donnclab.kinetiframe.ui.components.navigation.BottomNavDestination) -> Unit)? = null,
) {
    Scaffold(topBar = {
        Column {
            com.donnclab.kinetiframe.ui.components.navigation.AppBrandBar()
            TopAppBar(title = { Text("Settings") }, navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            })
        }
    }, bottomBar = {
        onNavigate?.let {
            com.donnclab.kinetiframe.ui.components.navigation.AppBottomNavBar(
                com.donnclab.kinetiframe.ui.components.navigation.BottomNavDestination.SETTINGS,
                it,
            )
        }
    }) { padding ->
        Column(
            Modifier.padding(padding).consumeWindowInsets(padding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text("Appearance", style = MaterialTheme.typography.titleSmall)
            PreferenceChoice(
                "Theme",
                state.preferences.theme,
                AppTheme.entries,
                !state.loading,
                {
                    when (it) {
                        AppTheme.SYSTEM -> "System"
                        AppTheme.LIGHT -> "Light"
                        AppTheme.DARK -> "Dark"
                    }
                },
                { onIntent(SettingsIntent.Theme(it)) },
            )
            HorizontalDivider()
            Text("Studio", style = MaterialTheme.typography.titleSmall)
            PreferenceSwitch(
                "Menu labels",
                "Small captions below menu icons.",
                state.preferences.showMenuLabels,
                !state.loading,
            ) { onIntent(SettingsIntent.MenuLabels(it)) }
            PreferenceChoice(
                "Orientation",
                state.preferences.orientation,
                StudioOrientation.entries,
                !state.loading,
                {
                    when (it) {
                        StudioOrientation.LANDSCAPE -> "Landscape"
                        StudioOrientation.PORTRAIT -> "Portrait"
                        StudioOrientation.DEVICE -> "Follow device"
                    }
                },
                { onIntent(SettingsIntent.Orientation(it)) },
            )
            PreferenceSwitch(
                "Hide Android bars",
                "Studio only; swipe from an edge to reveal them.",
                state.preferences.hideSystemBars,
                !state.loading,
            ) { onIntent(SettingsIntent.SystemBars(it)) }
            PreferenceSwitch(
                "Hide tools during playback",
                "Keep the canvas clear while previewing.",
                state.preferences.hideToolsDuringPlayback,
                !state.loading,
            ) {
                onIntent(SettingsIntent.PlaybackTools(it))
            }
            Text(
                "These preferences do not change project canvas size or export FPS.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            val context = androidx.compose.ui.platform.LocalContext.current
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Diagnostics", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                ContextIconButton(
                    ContextIconAction(Icons.Filled.BugReport, "Share diagnostics", {
                        com.donnclab.kinetiframe.ui.diagnostics.shareDiagnostics(context)
                    }),
                )
            }
        }
    }
}

@Composable
private fun <T> PreferenceChoice(
    label: String,
    value: T,
    choices: List<T>,
    enabled: Boolean,
    name: (T) -> String,
    onChoose: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Box {
            TextButton(onClick = { expanded = true }, enabled = enabled) { Text(name(value)) }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                choices.forEach { choice ->
                    DropdownMenuItem(
                        text = { Text(name(choice)) },
                        onClick = {
                            expanded = false
                            onChoose(choice)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PreferenceSwitch(
    label: String,
    hint: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    KinetiFrameTheme { SettingsScreen(SettingsState(loading = false), {}, {}) }
}
