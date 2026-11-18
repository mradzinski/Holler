package io.github.mradzinski.holler.sample.ui.settings.composable

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * One setting that is on or off: a title, a line explaining it and a switch. Pressing anywhere on the
 * row changes it.
 *
 * @param title the name of the setting.
 * @param description what the setting does.
 * @param checked whether the setting is on.
 * @param onCheckedChange called with the new value when the row is pressed.
 * @param modifier applied to the row.
 */
@Composable
internal fun SettingSwitch(
    title: StringResource,
    description: StringResource,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(description)) },
        // The row is the control, so the switch itself takes no presses and a screen reader sees one switch.
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}
