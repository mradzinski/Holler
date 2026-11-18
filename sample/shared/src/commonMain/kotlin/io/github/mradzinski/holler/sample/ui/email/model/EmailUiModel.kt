package io.github.mradzinski.holler.sample.ui.email.model

/**
 * An open email, as the email screen shows it.
 *
 * @property sender who sent the email.
 * @property subject its subject line.
 * @property body its text.
 */
internal data class EmailUiModel(
    val sender: String,
    val subject: String,
    val body: String,
)
