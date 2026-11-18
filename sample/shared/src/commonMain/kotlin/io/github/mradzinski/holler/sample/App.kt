package io.github.mradzinski.holler.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mradzinski.holler.Holler
import io.github.mradzinski.holler.sample.app.AppHollerHost
import io.github.mradzinski.holler.sample.app.AppNavigation
import io.github.mradzinski.holler.sample.app.AppViewModel
import io.github.mradzinski.holler.sample.ui.component.LocalIsOffline
import io.github.mradzinski.holler.sample.ui.theme.SampleTheme
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * The sample app, shared by every platform: a small mail inbox that shows how Holler is used in a real
 * app. Call the platform's `startSample` before drawing it.
 *
 * There is one [Holler] for the whole app, created by dependency injection. The view models
 * show their messages through it, and the one `HollerHost` here, placed over the navigation, draws
 * them. That is why a message outlives the screen that caused it: deleting an email closes its screen,
 * and "Email deleted" stays.
 *
 * @param platformEffects effects only one platform needs, run inside the app's theme. The web app uses
 *   it to tie the back stack to the browser's Back button.
 */
@Composable
fun App(platformEffects: @Composable () -> Unit = {}) {
    val viewModel = koinViewModel<AppViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val holler = koinInject<Holler>()

    // The app follows the system's light or dark theme until the user picks one in the settings.
    SampleTheme(dark = state.darkTheme ?: isSystemInDarkTheme()) {
        // Every screen draws the offline strip under its own top bar, so it is handed down to all of them.
        CompositionLocalProvider(LocalIsOffline provides state.offline) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                AppNavigation(holler)
                AppHollerHost(holler = holler, state = state, onEvent = viewModel::onEvent)
            }
        }

        platformEffects()
    }
}
