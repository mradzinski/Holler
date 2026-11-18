package io.github.mradzinski.holler.sample.ui.settings

import io.github.mradzinski.holler.sample.core.presentation.UiEvent

/** What the settings screen sends to [SettingsViewModel]. */
internal sealed interface SettingsEvent : UiEvent {

    /** The Back arrow was pressed. */
    data object OnBackClick : SettingsEvent

    /**
     * The dark theme switch was changed.
     *
     * @property enabled whether the dark theme is now on.
     */
    data class OnDarkThemeChange(val enabled: Boolean) : SettingsEvent

    /**
     * The switch for the custom look of messages was changed.
     *
     * @property enabled whether messages now use the brand look.
     */
    data class OnBrandLookChange(val enabled: Boolean) : SettingsEvent

    /**
     * The switch for the position of messages was changed.
     *
     * @property enabled whether messages now show at the top.
     */
    data class OnMessagesAtTopChange(val enabled: Boolean) : SettingsEvent

    /**
     * The switch for how messages are arranged was changed.
     *
     * @property enabled whether messages now show as a pile.
     */
    data class OnMessagesAsPileChange(val enabled: Boolean) : SettingsEvent

    /**
     * The "Simulate offline" switch was changed.
     *
     * @property enabled whether the app now behaves as if it were offline.
     */
    data class OnSimulateOfflineChange(val enabled: Boolean) : SettingsEvent

    /** The row that opens the playground was pressed. */
    data object OnPlaygroundClick : SettingsEvent
}
