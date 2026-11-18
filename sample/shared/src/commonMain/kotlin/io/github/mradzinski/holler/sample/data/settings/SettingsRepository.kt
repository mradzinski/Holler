package io.github.mradzinski.holler.sample.data.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Holds the user's [Settings]. */
internal interface SettingsRepository {

    /** The current settings, and every later change to them. */
    val settings: StateFlow<Settings>

    /**
     * Changes the settings.
     *
     * @param transform returns the new settings from the current ones.
     */
    fun update(transform: (Settings) -> Settings)
}

/** Keeps the settings in memory: they start from the defaults every time the app starts. */
internal class InMemorySettingsRepository : SettingsRepository {

    /** The settings, as the one value [update] changes. */
    private val state: MutableStateFlow<Settings> = MutableStateFlow(Settings())

    /** The current settings, and every later change to them. */
    override val settings: StateFlow<Settings> = state.asStateFlow()

    /**
     * Changes the settings.
     *
     * @param transform returns the new settings from the current ones.
     */
    override fun update(transform: (Settings) -> Settings) = state.update(transform)
}
