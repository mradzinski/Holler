package io.github.mradzinski.holler.sample.ui.inbox

import io.github.mradzinski.holler.sample.core.presentation.UiEvent

/** What the inbox screen sends to [InboxViewModel]. */
internal sealed interface InboxEvent : UiEvent {

    /**
     * A row was pressed.
     *
     * @property emailId the id of the row's email.
     */
    data class OnEmailClick(val emailId: Long) : InboxEvent

    /**
     * A row was swiped away, which deletes its email.
     *
     * @property emailId the id of the row's email.
     */
    data class OnEmailSwipe(val emailId: Long) : InboxEvent

    /** The list was pulled down to check for new emails. */
    data object OnRefreshPull : InboxEvent

    /** The button that checks for new emails was pressed. It is there where the list cannot be pulled. */
    data object OnRefreshClick : InboxEvent

    /** The settings button was pressed. */
    data object OnSettingsClick : InboxEvent
}
