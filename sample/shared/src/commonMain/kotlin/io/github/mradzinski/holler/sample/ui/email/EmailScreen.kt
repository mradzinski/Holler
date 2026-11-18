package io.github.mradzinski.holler.sample.ui.email

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import io.github.mradzinski.holler.sample.resources.Res
import io.github.mradzinski.holler.sample.resources.email_delete
import io.github.mradzinski.holler.sample.ui.component.SampleScaffold
import io.github.mradzinski.holler.sample.ui.component.SenderAvatar
import io.github.mradzinski.holler.sample.ui.email.composable.ReplyBar
import io.github.mradzinski.holler.sample.ui.email.model.EmailUiModel
import org.jetbrains.compose.resources.stringResource

/**
 * The email screen: one email, a button to delete it and a bar at the bottom to answer it. Pressing
 * anywhere on the email closes the keyboard, as mail apps do.
 *
 * @param state what to draw.
 * @param onEvent called with everything the user does on the screen.
 */
@Composable
internal fun EmailScreen(state: EmailUiState, onEvent: (EmailEvent) -> Unit) {
    val focusManager = LocalFocusManager.current

    SampleScaffold(
        // The subject is the headline of the content, where it has room to wrap, so the bar has no title.
        title = "",
        onBack = { onEvent(EmailEvent.OnBackClick) },
        actions = {
            IconButton(
                onClick = { onEvent(EmailEvent.OnDeleteClick) },
                enabled = state.email != null,
            ) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.email_delete))
            }
        },
        bottomBar = {
            ReplyBar(
                enabled = state.email != null,
                onSend = { onEvent(EmailEvent.OnSendClick(it)) },
            )
        },
    ) { innerPadding ->
        val email = state.email ?: return@SampleScaffold

        EmailContent(
            email = email,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Taking the focus away from the reply field is what closes the keyboard.
                .pointerInput(focusManager) { detectTapGestures { focusManager.clearFocus() } },
        )
    }
}

/**
 * The subject, the sender and the text of an email, which scroll when they do not fit.
 *
 * @param email the email to draw.
 * @param modifier applied to the scrolling area.
 */
@Composable
private fun EmailContent(email: EmailUiModel, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text = email.subject, style = MaterialTheme.typography.headlineSmall)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SenderAvatar(email.sender)
            Text(text = email.sender, style = MaterialTheme.typography.titleMedium)
        }

        Text(text = email.body, style = MaterialTheme.typography.bodyLarge)
    }
}
