package io.github.mradzinski.holler.sample.ui.settings

import io.github.mradzinski.holler.sample.core.presentation.UiState

/**
 * The state of the settings screen.
 *
 * @property darkTheme `true` or `false` once the user picked a theme; `null` while the app follows the system.
 * @property brandLook whether messages use the app's custom brand look.
 * @property messagesAtTop whether messages show at the top of the screen.
 * @property messagesAsPile whether messages show as a pile with the newest in front.
 * @property simulateOffline whether the app behaves as if the device had no network.
 */
internal data class SettingsUiState(
    val darkTheme: Boolean? = null,
    val brandLook: Boolean = false,
    val messagesAtTop: Boolean = false,
    val messagesAsPile: Boolean = false,
    val simulateOffline: Boolean = false,
) : UiState
