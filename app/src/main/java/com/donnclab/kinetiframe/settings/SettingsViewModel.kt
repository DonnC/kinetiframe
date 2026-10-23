package com.donnclab.kinetiframe.settings

import androidx.lifecycle.viewModelScope
import com.donnclab.kinetiframe.ui.BaseViewModel
import com.donnclab.kinetiframe.ui.ViewEffect
import com.donnclab.kinetiframe.ui.ViewIntent
import com.donnclab.kinetiframe.ui.ViewState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SettingsState(
    val preferences: AppPreferences = AppPreferences(),
    val loading: Boolean = true,
    val error: String? = null,
) : ViewState
sealed interface SettingsIntent : ViewIntent {
    data class Theme(val value: AppTheme) : SettingsIntent
    data class Orientation(val value: StudioOrientation) : SettingsIntent
    data class SystemBars(val hidden: Boolean) : SettingsIntent
    data class PlaybackTools(val hidden: Boolean) : SettingsIntent
    data class MenuLabels(val shown: Boolean) : SettingsIntent
}
sealed interface SettingsEffect : ViewEffect

class SettingsViewModel(private val store: AppPreferencesStore) :
    BaseViewModel<SettingsState, SettingsIntent, SettingsEffect>(SettingsState()) {
    private val writes = Mutex()
    init {
        viewModelScope.launch {
            writes.withLock {
                try {
                    val loaded = store.load()
                    updateState { it.copy(preferences = loaded, loading = false) }
                } catch (
                    cancelled: CancellationException,
                ) {
                    throw cancelled
                } catch (
                    _: Exception,
                ) {
                    updateState {
                        it.copy(loading = false, error = "Could not read settings. Your projects are unchanged.")
                    }
                }
            }
        }
    }
    override fun handleIntent(intent: SettingsIntent) {
        viewModelScope.launch {
            // Serialize rapid taps so an older disk write cannot overwrite a newer preference.
            writes.withLock {
                val next = when (intent) {
                    is SettingsIntent.Theme -> currentState.preferences.copy(theme = intent.value)

                    is SettingsIntent.Orientation -> currentState.preferences.copy(orientation = intent.value)

                    is SettingsIntent.SystemBars -> currentState.preferences.copy(hideSystemBars = intent.hidden)

                    is SettingsIntent.PlaybackTools -> currentState.preferences.copy(
                        hideToolsDuringPlayback = intent.hidden,
                    )

                    is SettingsIntent.MenuLabels -> currentState.preferences.copy(showMenuLabels = intent.shown)
                }
                try {
                    store.save(next)
                    updateState { it.copy(preferences = next, error = null) }
                } catch (
                    cancelled: CancellationException,
                ) {
                    throw cancelled
                } catch (
                    _: Exception,
                ) {
                    updateState { it.copy(error = "Settings could not be saved. Please try again.") }
                }
            }
        }
    }
}
