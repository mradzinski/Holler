package io.github.mradzinski.holler.sample.ui.email

import io.github.mradzinski.holler.sample.core.presentation.UiEvent

/** What the email screen sends to [EmailViewModel]. */
internal sealed interface EmailEvent : UiEvent {

    /** The Back arrow was pressed. */
    data object OnBackClick : EmailEvent

    /** The delete button was pressed. */
    data object OnDeleteClick : EmailEvent

    /**
     * The send button was pressed.
     *
     * @property text the reply the user wrote.
     */
    data class OnSendClick(val text: String) : EmailEvent
}
