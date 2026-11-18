package io.github.mradzinski.holler.sample.ui.email

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mradzinski.holler.Holler
import io.github.mradzinski.holler.sample.core.navigation.Navigator
import io.github.mradzinski.holler.sample.core.presentation.WhileScreenIsWatching
import io.github.mradzinski.holler.sample.data.email.EmailRepository
import io.github.mradzinski.holler.sample.messages.AppMessage
import io.github.mradzinski.holler.sample.ui.email.mapper.toUiModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The view model of the email screen. Opening the screen marks its email as read.
 *
 * @param emailId the id of the email the screen shows.
 * @param emails where the email comes from, and what deletes and answers it.
 * @param holler shows "Email deleted", which stays on screen after this screen has closed.
 * @param navigator closes the screen.
 */
internal class EmailViewModel(
    private val emailId: Long,
    private val emails: EmailRepository,
    private val holler: Holler,
    private val navigator: Navigator,
) : ViewModel() {

    /**
     * The state the email screen draws. A deleted email is no longer in the inbox, and the state then keeps
     * the last email it had, so the screen does not go blank while it slides away.
     */
    val state: StateFlow<EmailUiState> = emails.observeEmail(emailId)
        .filterNotNull()
        .map { email -> EmailUiState(email = email.toUiModel()) }
        .stateIn(viewModelScope, WhileScreenIsWatching, EmailUiState())

    init {
        viewModelScope.launch { emails.markRead(emailId) }
    }

    /**
     * Handles an event of the email screen.
     *
     * @param event the event.
     */
    fun onEvent(event: EmailEvent) {
        when (event) {
            is EmailEvent.OnBackClick -> navigator.goBack()
            is EmailEvent.OnDeleteClick -> delete()
            is EmailEvent.OnSendClick -> emails.sendReply(emailId, event.text)
        }
    }

    /**
     * Deletes the email, shows the "Email deleted" message and closes the screen. The message is shown
     * as plain data through the app's one host state, so it stays on screen after this screen and this
     * view model are gone. Its Undo is handled by the app's root for the same reason.
     */
    private fun delete() {
        viewModelScope.launch {
            emails.delete(emailId)
            holler.show(AppMessage.EmailDeleted(emailId))
            navigator.goBack()
        }
    }
}
