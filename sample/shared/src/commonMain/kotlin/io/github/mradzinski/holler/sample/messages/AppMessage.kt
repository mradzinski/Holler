package io.github.mradzinski.holler.sample.messages

/**
 * The messages the app shows on screen, as plain data: no text, no icon, no Compose. A view model shows
 * one with `holler.show(AppMessage.…)`; the app's `HollerHost` gives each its look, in `AppHollerHost`.
 * Adding a message means adding a subtype here and its look there.
 */
internal sealed interface AppMessage {

    /**
     * An email was deleted. The message offers to undo it.
     *
     * @property emailId the id of the deleted email, which Undo puts back.
     */
    data class EmailDeleted(val emailId: Long) : AppMessage

    /** Checking for new emails failed. The message offers to try again. */
    data object RefreshFailed : AppMessage

    /**
     * The connection was just lost. It is a short notice of the change: the lasting sign that the app is
     * offline is the strip under the top bar, not a message.
     */
    data object Offline : AppMessage

    /** A reply is on its way. */
    data object ReplySending : AppMessage

    /** The reply was sent. It takes the place of [ReplySending]. */
    data object ReplySent : AppMessage

    /** The reply could not be sent. It takes the place of [ReplySending]. */
    data object ReplyFailed : AppMessage
}

/** The key of the [AppMessage.Offline] message, used to close it if the connection comes back while it shows. */
internal const val OFFLINE_KEY: String = "offline"

/** The key shared by the messages about a reply, which makes each replace the one before it. */
internal const val REPLY_KEY: String = "reply"
