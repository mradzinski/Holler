package io.github.mradzinski.holler.sample.data.settings

/**
 * What the user chose on the settings screen.
 *
 * @property darkTheme `true` or `false` once the user picked a theme; `null` to follow the system.
 * @property brandLook whether messages use the app's custom brand look, not the app's Material theme.
 * @property messagesAtTop whether messages show at the top of the screen, not at the bottom.
 * @property messagesAsPile whether messages show as a pile with the newest in front, not one under the other.
 * @property simulateOffline whether the app behaves as if the device had no network.
 */
internal data class Settings(
    val darkTheme: Boolean? = null,
    val brandLook: Boolean = false,
    val messagesAtTop: Boolean = false,
    val messagesAsPile: Boolean = false,
    val simulateOffline: Boolean = false,
)
