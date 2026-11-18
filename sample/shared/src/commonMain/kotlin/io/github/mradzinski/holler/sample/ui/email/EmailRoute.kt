package io.github.mradzinski.holler.sample.ui.email

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mradzinski.holler.sample.core.navigation.EmailKey
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The email screen wired to its view model: it collects the state and passes events on.
 *
 * @param key the navigation key of the screen, which names the email to show.
 */
@Composable
internal fun EmailRoute(key: EmailKey) {
    val viewModel = koinViewModel<EmailViewModel> { parametersOf(key.emailId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    EmailScreen(state = state, onEvent = viewModel::onEvent)
}
