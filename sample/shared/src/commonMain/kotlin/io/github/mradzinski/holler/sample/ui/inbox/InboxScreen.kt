package io.github.mradzinski.holler.sample.ui.inbox

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.mradzinski.holler.sample.resources.Res
import io.github.mradzinski.holler.sample.resources.inbox_empty
import io.github.mradzinski.holler.sample.resources.inbox_refresh
import io.github.mradzinski.holler.sample.resources.inbox_settings
import io.github.mradzinski.holler.sample.resources.inbox_title
import io.github.mradzinski.holler.sample.ui.component.SampleScaffold
import io.github.mradzinski.holler.sample.ui.component.onlyBottom
import io.github.mradzinski.holler.sample.ui.component.withoutBottom
import io.github.mradzinski.holler.sample.ui.inbox.composable.EmailRow
import io.github.mradzinski.holler.sample.ui.inbox.composable.SwipeToDelete
import io.github.mradzinski.holler.sample.ui.inbox.model.EmailItemUiModel
import org.jetbrains.compose.resources.stringResource

/**
 * The inbox screen: the list of emails, with buttons to check for new ones and to open the settings.
 * A row opens its email when pressed and deletes it when swiped away. The screen holds no state of
 * its own.
 *
 * @param state what to draw.
 * @param onEvent called with everything the user does on the screen.
 */
@Composable
internal fun InboxScreen(state: InboxUiState, onEvent: (InboxEvent) -> Unit) {
    SampleScaffold(
        title = stringResource(Res.string.inbox_title),
        actions = {
            IconButton(onClick = { onEvent(InboxEvent.OnRefreshClick) }) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.inbox_refresh))
            }

            IconButton(onClick = { onEvent(InboxEvent.OnSettingsClick) }) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(Res.string.inbox_settings))
            }
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding.withoutBottom())) {
            if (state.isRefreshing) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            if (state.emails.isEmpty()) {
                EmptyInbox()
            } else {
                EmailList(
                    emails = state.emails,
                    contentPadding = innerPadding.onlyBottom(),
                    onEvent = onEvent,
                )
            }
        }
    }
}

/**
 * The rows of the inbox. A row that is deleted, or put back by Undo, slides into place.
 *
 * @param emails the rows, newest first.
 * @param contentPadding the space left after the last row, so it can be scrolled clear of the system's bottom inset.
 * @param onEvent called when a row is pressed or swiped away.
 */
@Composable
private fun EmailList(
    emails: List<EmailItemUiModel>,
    contentPadding: PaddingValues,
    onEvent: (InboxEvent) -> Unit,
) {
    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize(),
    ) {
        items(emails, key = { it.id }) { email ->
            SwipeToDelete(
                onDelete = { onEvent(InboxEvent.OnEmailSwipe(email.id)) },
                modifier = Modifier.animateItem(),
            ) {
                EmailRow(
                    email = email,
                    onClick = { onEvent(InboxEvent.OnEmailClick(email.id)) },
                )
            }
        }
    }
}

/** What the inbox shows when it has no emails. */
@Composable
private fun EmptyInbox() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(
            text = stringResource(Res.string.inbox_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
