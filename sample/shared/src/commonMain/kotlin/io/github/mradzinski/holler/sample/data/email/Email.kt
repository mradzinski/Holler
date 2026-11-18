package io.github.mradzinski.holler.sample.data.email

/**
 * An email of the inbox.
 *
 * @property id identifies the email.
 * @property sender who sent it.
 * @property subject its subject line.
 * @property body its text.
 * @property isRead whether the user has opened it.
 */
internal data class Email(
    val id: Long,
    val sender: String,
    val subject: String,
    val body: String,
    val isRead: Boolean = false,
)

/** Where the reply the user last sent stands. */
internal enum class ReplyStatus {
    /** No reply is being sent, and there is no result waiting to be shown. */
    Idle,

    /** A reply is on its way. */
    Sending,

    /** The last reply was sent. */
    Sent,

    /** The last reply could not be sent. */
    Failed,
}

/** Thrown by an operation that needs the network while the app is offline. */
internal class OfflineException : Exception("The device is offline.")
