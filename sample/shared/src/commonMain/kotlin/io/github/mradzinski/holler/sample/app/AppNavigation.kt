package io.github.mradzinski.holler.sample.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import io.github.mradzinski.holler.Holler
import io.github.mradzinski.holler.sample.core.navigation.EmailKey
import io.github.mradzinski.holler.sample.core.navigation.InboxKey
import io.github.mradzinski.holler.sample.core.navigation.Navigator
import io.github.mradzinski.holler.sample.core.navigation.NavigatorState
import io.github.mradzinski.holler.sample.core.navigation.PlaygroundKey
import io.github.mradzinski.holler.sample.core.navigation.SettingsKey
import io.github.mradzinski.holler.sample.resources.Res
import io.github.mradzinski.holler.sample.resources.inbox_no_selection
import io.github.mradzinski.holler.sample.ui.email.EmailRoute
import io.github.mradzinski.holler.sample.ui.inbox.InboxRoute
import io.github.mradzinski.holler.sample.ui.playground.PlaygroundRoute
import io.github.mradzinski.holler.sample.ui.settings.SettingsRoute
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * The screens of the app, drawn from the [Navigator]'s back stack. In a wide window the inbox and the
 * open email sit side by side; in a narrow one the email covers the inbox.
 *
 * @param holler the host state of the app's `HollerHost`, which only the playground uses directly.
 * @param modifier applied to the navigation area.
 */
// The list-detail layout for Navigation 3 is still marked experimental in Material's adaptive library.
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun AppNavigation(holler: Holler, modifier: Modifier = Modifier) {
    val navigator = koinInject<Navigator>()
    val navigatorState = koinInject<NavigatorState>()

    // Saves the back stack with the rest of the saved state, and puts it back when the system recreates the app.
    rememberSaveable(saver = backStackSaver(navigatorState)) { navigatorState }

    NavDisplay(
        backStack = navigator.backStack,
        modifier = modifier,
        onBack = { navigator.goBack() },
        sceneStrategies = listOf(
            // The list-detail layout also handles the one-pane case. The inbox then lives in the same layout
            // whatever the window's size, and a rotation resizes that layout instead of moving the inbox
            // from one layout to another while its rows are being measured.
            rememberListDetailSceneStrategy(
                shouldHandleSinglePaneLayout = true,
                directive = paneDirective(),
            ),
        ),
        // Each screen on the back stack keeps its own saved state and its own view models.
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<InboxKey>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { NoEmailSelected() },
                ),
            ) {
                InboxRoute()
            }

            entry<EmailKey>(metadata = ListDetailSceneStrategy.detailPane()) { key -> EmailRoute(key) }
            entry<SettingsKey> { SettingsRoute() }
            entry<PlaygroundKey> { PlaygroundRoute(holler) }
        },
    )
}

/**
 * Decides how many panes fit side by side. It is Material's own rule, which looks at the window's width,
 * with one addition: a window that is short shows one pane however wide it is. A phone on its side is
 * wide enough for two panes by width alone, but too short to share: with the keyboard open, an email
 * next to the inbox would have no room left.
 *
 * @return the directive the list-detail layout follows.
 */
// Material's adaptive library marks the function that computes its default directive as experimental.
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun paneDirective(): PaneScaffoldDirective {
    val windowInfo = currentWindowAdaptiveInfoV2()
    val directive = calculatePaneScaffoldDirective(windowInfo)
    val tallEnough = windowInfo.windowSizeClass.isHeightAtLeastBreakpoint(
        WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND,
    )

    return if (tallEnough) {
        directive
    } else {
        directive.copy(maxHorizontalPartitions = 1)
    }
}

/** What a wide window shows next to the inbox while no email is open. */
@Composable
private fun NoEmailSelected() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(
            text = stringResource(Res.string.inbox_no_selection),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Saves the back stack as text and restores it into the same navigator.
 *
 * @param state the navigator, seen as something that can be saved.
 * @return the saver.
 */
private fun backStackSaver(state: NavigatorState): Saver<NavigatorState, String> = Saver(
    save = { it.save() },
    restore = { saved -> state.apply { restore(saved) } },
)
