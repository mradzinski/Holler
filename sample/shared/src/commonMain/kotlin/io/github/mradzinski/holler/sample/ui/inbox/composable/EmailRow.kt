package io.github.mradzinski.holler.sample.ui.inbox.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mradzinski.holler.sample.resources.Res
import io.github.mradzinski.holler.sample.resources.inbox_unread
import io.github.mradzinski.holler.sample.ui.component.SenderAvatar
import io.github.mradzinski.holler.sample.ui.inbox.model.EmailItemUiModel
import org.jetbrains.compose.resources.stringResource

/** The diameter of the dot that marks an unread email. */
private val UnreadDotSize: Dp = 10.dp

/**
 * One row of the inbox, laid out as a Material three-line list item: the sender's avatar, then the
 * sender, the subject and a one-line preview. An unread email has its sender and subject in bold, and a
 * dot at the end.
 *
 * It is built from a plain `Row`, not from Material's `ListItem`. `ListItem` reads the baselines of its
 * texts while it is measured, and Compose 1.10 crashes on that when the list is animating a row that
 * was just removed, which is what a swipe to delete does.
 *
 * @param email the row to draw.
 * @param onClick called when the row is pressed.
 * @param modifier applied to the row.
 */
@Composable
internal fun EmailRow(
    email: EmailItemUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        SenderAvatar(email.sender)

        EmailRowTexts(
            email = email,
            modifier = Modifier.weight(1f),
        )

        if (email.isUnread) {
            UnreadDot()
        }
    }
}

/**
 * The three lines of a row: sender, subject and preview, each cut to one line.
 *
 * @param email the row to draw.
 * @param modifier applied to the column of texts.
 */
@Composable
private fun EmailRowTexts(email: EmailItemUiModel, modifier: Modifier = Modifier) {
    val weight = if (email.isUnread) {
        FontWeight.Bold
    } else {
        FontWeight.Normal
    }

    Column(modifier) {
        Text(
            text = email.sender,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = weight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = email.subject,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = weight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = email.preview,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The dot at the end of the row of an email that has not been opened. */
@Composable
private fun UnreadDot() {
    val description = stringResource(Res.string.inbox_unread)

    Box(
        Modifier
            .size(UnreadDotSize)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .semantics { contentDescription = description },
    )
}
