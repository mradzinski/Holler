package io.github.mradzinski.holler.sample.ui.inbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/** The inbox screen wired to its view model: it collects the state and passes events on. */
@Composable
internal fun InboxRoute() {
    val viewModel = koinViewModel<InboxViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    InboxScreen(state = state, onEvent = viewModel::onEvent)
}
