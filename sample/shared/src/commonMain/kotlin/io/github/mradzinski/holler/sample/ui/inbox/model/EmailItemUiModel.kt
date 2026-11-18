package io.github.mradzinski.holler.sample.ui.inbox.model

/**
 * One row of the inbox.
 *
 * @property id the id of the email, sent back when the row is pressed.
 * @property sender who sent the email.
 * @property subject its subject line.
 * @property preview the start of its text, on one line.
 * @property isUnread whether the email has not been opened yet, which the row shows in bold.
 */
internal data class EmailItemUiModel(
    val id: Long,
    val sender: String,
    val subject: String,
    val preview: String,
    val isUnread: Boolean,
)
