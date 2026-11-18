package io.github.mradzinski.holler.sample.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/** The settings screen wired to its view model: it collects the state and passes events on. */
@Composable
internal fun SettingsRoute() {
    val viewModel = koinViewModel<SettingsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsScreen(state = state, onEvent = viewModel::onEvent)
}
