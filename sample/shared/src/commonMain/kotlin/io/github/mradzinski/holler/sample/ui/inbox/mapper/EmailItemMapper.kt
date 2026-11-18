package io.github.mradzinski.holler.sample.ui.inbox.mapper

import io.github.mradzinski.holler.sample.data.email.Email
import io.github.mradzinski.holler.sample.ui.inbox.model.EmailItemUiModel

/** How many characters of an email's text its row previews. */
private const val PREVIEW_LENGTH: Int = 90

/**
 * Turns an email into its row of the inbox.
 *
 * @return the row, with the email's text cut to a one-line preview.
 */
internal fun Email.toItemUiModel(): EmailItemUiModel = EmailItemUiModel(
    id = id,
    sender = sender,
    subject = subject,
    preview = body
        .lineSequence()
        .filter { it.isNotBlank() }
        .joinToString(separator = " ")
        .take(PREVIEW_LENGTH),
    isUnread = !isRead,
)
