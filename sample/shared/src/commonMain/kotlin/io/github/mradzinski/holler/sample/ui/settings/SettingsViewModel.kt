package io.github.mradzinski.holler.sample.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mradzinski.holler.sample.core.navigation.Navigator
import io.github.mradzinski.holler.sample.core.navigation.PlaygroundKey
import io.github.mradzinski.holler.sample.core.presentation.WhileScreenIsWatching
import io.github.mradzinski.holler.sample.data.settings.SettingsRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The view model of the settings screen.
 *
 * @param settings where the settings are read from and written to.
 * @param navigator closes the screen and opens the playground.
 */
internal class SettingsViewModel(private val settings: SettingsRepository, private val navigator: Navigator) : ViewModel() {

    /** The state the settings screen draws. */
    val state: StateFlow<SettingsUiState> = settings.settings
        .map { current ->
            SettingsUiState(
                darkTheme = current.darkTheme,
                brandLook = current.brandLook,
                messagesAtTop = current.messagesAtTop,
                messagesAsPile = current.messagesAsPile,
                simulateOffline = current.simulateOffline,
            )
        }
        .stateIn(viewModelScope, WhileScreenIsWatching, SettingsUiState())

    /**
     * Handles an event of the settings screen.
     *
     * @param event the event.
     */
    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.OnBackClick -> navigator.goBack()

            is SettingsEvent.OnDarkThemeChange -> settings.update {
                it.copy(darkTheme = event.enabled)
            }

            is SettingsEvent.OnBrandLookChange -> settings.update {
                it.copy(brandLook = event.enabled)
            }

            is SettingsEvent.OnMessagesAtTopChange -> settings.update {
                it.copy(messagesAtTop = event.enabled)
            }

            is SettingsEvent.OnMessagesAsPileChange -> settings.update {
                it.copy(messagesAsPile = event.enabled)
            }

            is SettingsEvent.OnSimulateOfflineChange -> settings.update {
                it.copy(simulateOffline = event.enabled)
            }

            is SettingsEvent.OnPlaygroundClick -> navigator.navigate(PlaygroundKey)
        }
    }
}
