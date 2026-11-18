package io.github.mradzinski.holler.sample.ui.email.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.mradzinski.holler.sample.resources.Res
import io.github.mradzinski.holler.sample.resources.email_reply_hint
import io.github.mradzinski.holler.sample.resources.email_send
import org.jetbrains.compose.resources.stringResource

/** How many lines the reply field grows to before its text scrolls. */
private const val MAX_REPLY_LINES: Int = 4

/**
 * The bar at the bottom of the email screen: a field to write a reply in and a send button. It stays
 * above the keyboard and the system's navigation bar.
 *
 * The text being typed is the field's own state: the view model only hears about it when it is sent.
 * Sending empties the field and closes the keyboard.
 *
 * @param enabled whether a reply can be written, which it cannot while there is no email.
 * @param onSend called with the reply when it is sent, from the button or from the keyboard.
 * @param modifier applied to the bar.
 */
@Composable
internal fun ReplyBar(
    enabled: Boolean,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var draft by rememberSaveable { mutableStateOf("") }

    val send = {
        if (draft.isNotBlank()) {
            onSend(draft.trim())
            draft = ""
            focusManager.clearFocus()
        }
    }

    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            TextField(
                value = draft,
                onValueChange = { draft = it },
                enabled = enabled,
                placeholder = { Text(stringResource(Res.string.email_reply_hint)) },
                maxLines = MAX_REPLY_LINES,
                // A pill with no underline, which is how Material draws a field inside a bar.
                shape = MaterialTheme.shapes.extraLarge,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send,
                ),
                keyboardActions = KeyboardActions(onSend = { send() }),
                modifier = Modifier.weight(1f),
            )

            FilledIconButton(
                onClick = send,
                enabled = enabled && draft.isNotBlank(),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(Res.string.email_send),
                )
            }
        }
    }
}
