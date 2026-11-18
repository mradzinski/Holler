package io.github.mradzinski.holler.sample.app

import io.github.mradzinski.holler.sample.core.presentation.UiEvent

/** What the app's root sends to [AppViewModel]: the actions of the messages on screen. */
internal sealed interface AppEvent : UiEvent {

    /**
     * Undo was pressed on the "Email deleted" message.
     *
     * @property emailId the id of the email to put back.
     */
    data class OnUndoDeleteClick(val emailId: Long) : AppEvent

    /** Retry was pressed on the message saying that checking for new emails failed. */
    data object OnRetryRefreshClick : AppEvent
}
