package io.github.mradzinski.holler.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import com.github.terrakok.navigation3.browser.HierarchicalBrowserNavigation
import com.github.terrakok.navigation3.browser.buildBrowserHistoryFragment
import io.github.mradzinski.holler.sample.core.navigation.EmailKey
import io.github.mradzinski.holler.sample.core.navigation.InboxKey
import io.github.mradzinski.holler.sample.core.navigation.Navigator
import io.github.mradzinski.holler.sample.core.navigation.PlaygroundKey
import io.github.mradzinski.holler.sample.core.navigation.SettingsKey
import org.koin.compose.koinInject

/**
 * Ties the app's back stack to the browser: the browser's Back button closes the visible screen, as the
 * app's own Back arrow does, and the address bar names the screen. Pass it to `App` as its platform
 * effects.
 *
 * Navigation 3 does not do this by itself yet (JetBrains tracks it as CMP-8924). Until it does, this
 * uses the small library JetBrains' own documentation points to. Forward and typing an address are
 * not supported by it.
 */
@Composable
fun BrowserHistory() {
    val navigator = koinInject<Navigator>()
    val current = remember(navigator) {
        derivedStateOf { navigator.backStack.last() }
    }

    HierarchicalBrowserNavigation(currentDestination = current, currentDestinationName = ::fragmentOf)
}

/**
 * Names a screen in the address bar.
 *
 * @param key the navigation key of the screen.
 * @return the part of the address after `#`, or `null` for a screen with no name.
 */
private fun fragmentOf(key: NavKey): String? = when (key) {
    is InboxKey -> buildBrowserHistoryFragment("inbox")
    is EmailKey -> buildBrowserHistoryFragment("email", mapOf("id" to key.emailId.toString()))
    is SettingsKey -> buildBrowserHistoryFragment("settings")
    is PlaygroundKey -> buildBrowserHistoryFragment("playground")
    else -> null
}
