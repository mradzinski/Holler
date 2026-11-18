package io.github.mradzinski.holler.sample.app

import io.github.mradzinski.holler.sample.core.presentation.UiState

/**
 * The state of the app's root: what every screen shares.
 *
 * @property darkTheme `true` or `false` once the user picked a theme; `null` to follow the system.
 * @property brandLook whether messages use the app's custom brand look.
 * @property messagesAtTop whether messages show at the top of the screen.
 * @property messagesAsPile whether messages show as a pile with the newest in front.
 * @property offline whether the app is offline, which every screen shows as a strip under its top bar.
 */
internal data class AppUiState(
    val darkTheme: Boolean? = null,
    val brandLook: Boolean = false,
    val messagesAtTop: Boolean = false,
    val messagesAsPile: Boolean = false,
    val offline: Boolean = false,
) : UiState
