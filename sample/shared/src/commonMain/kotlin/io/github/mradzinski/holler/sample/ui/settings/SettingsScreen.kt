package io.github.mradzinski.holler.sample.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.mradzinski.holler.sample.resources.Res
import io.github.mradzinski.holler.sample.resources.settings_brand_look
import io.github.mradzinski.holler.sample.resources.settings_brand_look_description
import io.github.mradzinski.holler.sample.resources.settings_dark_theme
import io.github.mradzinski.holler.sample.resources.settings_dark_theme_description
import io.github.mradzinski.holler.sample.resources.settings_messages_as_pile
import io.github.mradzinski.holler.sample.resources.settings_messages_as_pile_description
import io.github.mradzinski.holler.sample.resources.settings_messages_at_top
import io.github.mradzinski.holler.sample.resources.settings_messages_at_top_description
import io.github.mradzinski.holler.sample.resources.settings_playground
import io.github.mradzinski.holler.sample.resources.settings_playground_description
import io.github.mradzinski.holler.sample.resources.settings_simulate_offline
import io.github.mradzinski.holler.sample.resources.settings_simulate_offline_description
import io.github.mradzinski.holler.sample.resources.settings_title
import io.github.mradzinski.holler.sample.ui.component.SampleScaffold
import io.github.mradzinski.holler.sample.ui.component.onlyBottom
import io.github.mradzinski.holler.sample.ui.component.withoutBottom
import io.github.mradzinski.holler.sample.ui.settings.composable.SettingSwitch
import org.jetbrains.compose.resources.stringResource

/**
 * The settings screen: the app's theme, the look and position of its messages, a switch that pretends
 * the device is offline, and the way into the playground.
 *
 * @param state what to draw.
 * @param onEvent called with everything the user does on the screen.
 */
@Composable
internal fun SettingsScreen(state: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    SampleScaffold(
        title = stringResource(Res.string.settings_title),
        onBack = { onEvent(SettingsEvent.OnBackClick) },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding.withoutBottom())
                .verticalScroll(rememberScrollState())
                .padding(innerPadding.onlyBottom()),
        ) {
            SettingSwitch(
                title = Res.string.settings_dark_theme,
                description = Res.string.settings_dark_theme_description,
                // Until the user picks a theme, the switch shows the one the system uses.
                checked = state.darkTheme ?: isSystemInDarkTheme(),
                onCheckedChange = { onEvent(SettingsEvent.OnDarkThemeChange(it)) },
            )

            SettingSwitch(
                title = Res.string.settings_brand_look,
                description = Res.string.settings_brand_look_description,
                checked = state.brandLook,
                onCheckedChange = { onEvent(SettingsEvent.OnBrandLookChange(it)) },
            )

            SettingSwitch(
                title = Res.string.settings_messages_at_top,
                description = Res.string.settings_messages_at_top_description,
                checked = state.messagesAtTop,
                onCheckedChange = { onEvent(SettingsEvent.OnMessagesAtTopChange(it)) },
            )

            SettingSwitch(
                title = Res.string.settings_messages_as_pile,
                description = Res.string.settings_messages_as_pile_description,
                checked = state.messagesAsPile,
                onCheckedChange = { onEvent(SettingsEvent.OnMessagesAsPileChange(it)) },
            )

            SettingSwitch(
                title = Res.string.settings_simulate_offline,
                description = Res.string.settings_simulate_offline_description,
                checked = state.simulateOffline,
                onCheckedChange = { onEvent(SettingsEvent.OnSimulateOfflineChange(it)) },
            )

            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(Res.string.settings_playground)) },
                supportingContent = { Text(stringResource(Res.string.settings_playground_description)) },
                modifier = Modifier.clickable { onEvent(SettingsEvent.OnPlaygroundClick) },
            )
        }
    }
}
