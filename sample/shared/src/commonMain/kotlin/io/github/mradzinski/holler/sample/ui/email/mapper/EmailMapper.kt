package io.github.mradzinski.holler.sample.ui.email.mapper

import io.github.mradzinski.holler.sample.data.email.Email
import io.github.mradzinski.holler.sample.ui.email.model.EmailUiModel

/**
 * Turns an email into what the email screen shows.
 *
 * @return the email's sender, subject and text.
 */
internal fun Email.toUiModel(): EmailUiModel = EmailUiModel(
    sender = sender,
    subject = subject,
    body = body,
)
