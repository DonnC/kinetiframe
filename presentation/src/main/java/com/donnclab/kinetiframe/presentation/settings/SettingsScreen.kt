package com.donnclab.kinetiframe.presentation.settings

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.preferences.model.AppTheme
import com.donnclab.kinetiframe.preferences.model.StudioOrientation
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.ContextIconAction
import com.donnclab.kinetiframe.presentation.common.ContextIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
    onNavigate: ((com.donnclab.kinetiframe.presentation.common.BottomNavDestination) -> Unit)? = null,
    onShareDiagnostics: () -> Unit = {},
) {
    Scaffold(topBar = {
        Column {
            com.donnclab.kinetiframe.presentation.common.AppBrandBar()
            TopAppBar(title = { Text(stringResource(R.string.settings_title)) }, navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                }
            })
        }
    }, bottomBar = {
        onNavigate?.let {
            com.donnclab.kinetiframe.presentation.common.AppBottomNavBar(
                com.donnclab.kinetiframe.presentation.common.BottomNavDestination.SETTINGS,
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
            Text(stringResource(R.string.settings_appearance), style = MaterialTheme.typography.titleSmall)
            PreferenceChoice(
                stringResource(R.string.settings_theme),
                state.preferences.theme,
                AppTheme.entries,
                !state.loading,
                {
                    when (it) {
                        AppTheme.SYSTEM -> stringResource(R.string.settings_theme_system)
                        AppTheme.LIGHT -> stringResource(R.string.settings_theme_light)
                        AppTheme.DARK -> stringResource(R.string.settings_theme_dark)
                    }
                },
                { onIntent(SettingsIntent.Theme(it)) },
            )
            HorizontalDivider()
            Text(stringResource(R.string.settings_studio), style = MaterialTheme.typography.titleSmall)
            PreferenceSwitch(
                stringResource(R.string.settings_menu_labels),
                stringResource(R.string.settings_menu_labels_hint),
                state.preferences.showMenuLabels,
                !state.loading,
            ) { onIntent(SettingsIntent.MenuLabels(it)) }
            PreferenceChoice(
                stringResource(R.string.settings_orientation),
                state.preferences.orientation,
                StudioOrientation.entries,
                !state.loading,
                {
                    when (it) {
                        StudioOrientation.LANDSCAPE -> stringResource(R.string.settings_orientation_landscape)
                        StudioOrientation.PORTRAIT -> stringResource(R.string.settings_orientation_portrait)
                        StudioOrientation.DEVICE -> stringResource(R.string.settings_orientation_device)
                    }
                },
                { onIntent(SettingsIntent.Orientation(it)) },
            )
            PreferenceSwitch(
                stringResource(R.string.settings_hide_system_bars),
                stringResource(R.string.settings_hide_system_bars_hint),
                state.preferences.hideSystemBars,
                !state.loading,
            ) { onIntent(SettingsIntent.SystemBars(it)) }
            PreferenceSwitch(
                stringResource(R.string.settings_hide_tools),
                stringResource(R.string.settings_hide_tools_hint),
                state.preferences.hideToolsDuringPlayback,
                !state.loading,
            ) {
                onIntent(SettingsIntent.PlaybackTools(it))
            }
            Text(
                stringResource(R.string.settings_scope_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.settings_diagnostics),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                )
                ContextIconButton(
                    ContextIconAction(Icons.Filled.BugReport, stringResource(R.string.settings_share_diagnostics), {
                        onShareDiagnostics()
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
    name: @Composable (T) -> String,
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
