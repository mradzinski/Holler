package io.github.mradzinski.holler.sample.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mradzinski.holler.Holler
import io.github.mradzinski.holler.sample.core.input.DeviceInput
import io.github.mradzinski.holler.sample.core.navigation.EmailKey
import io.github.mradzinski.holler.sample.core.navigation.InboxKey
import io.github.mradzinski.holler.sample.core.navigation.Navigator
import io.github.mradzinski.holler.sample.core.navigation.SettingsKey
import io.github.mradzinski.holler.sample.core.presentation.WhileScreenIsWatching
import io.github.mradzinski.holler.sample.data.email.EmailRepository
import io.github.mradzinski.holler.sample.messages.AppMessage
import io.github.mradzinski.holler.sample.ui.inbox.mapper.toItemUiModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The view model of the inbox screen.
 *
 * @param emails where the emails come from.
 * @param holler shows the messages for a deleted email and a failed refresh.
 * @param navigator opens the other screens.
 * @param input tells whether the device has a touch screen, which decides if the screen needs a refresh button.
 */
internal class InboxViewModel(
    private val emails: EmailRepository,
    private val holler: Holler,
    private val navigator: Navigator,
    private val input: DeviceInput,
) : ViewModel() {

    /** The state the inbox screen draws. */
    val state: StateFlow<InboxUiState> = combine(emails.inbox, emails.isRefreshing) { inbox, refreshing ->
        InboxUiState(
            emails = inbox.map { it.toItemUiModel() },
            isRefreshing = refreshing,
            showRefreshButton = !input.hasTouchScreen,
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileScreenIsWatching,
        initialValue = InboxUiState(showRefreshButton = !input.hasTouchScreen),
    )

    /**
     * Handles an event of the inbox screen.
     *
     * @param event the event.
     */
    fun onEvent(event: InboxEvent) {
        when (event) {
            is InboxEvent.OnEmailClick -> openEmail(event.emailId)
            is InboxEvent.OnEmailSwipe -> delete(event.emailId)
            is InboxEvent.OnRefreshPull -> refresh()
            is InboxEvent.OnRefreshClick -> refresh()
            is InboxEvent.OnSettingsClick -> navigator.navigate(SettingsKey)
        }
    }

    /**
     * Opens an email. An email that is already open is closed first, so a wide window swaps the email
     * next to the inbox and Back still leads to the inbox alone.
     *
     * @param emailId the id of the email.
     */
    private fun openEmail(emailId: Long) {
        navigator.popUpTo { it == InboxKey }
        navigator.navigate(EmailKey(emailId))
    }

    /**
     * Deletes an email and shows the "Email deleted" message, whose Undo puts it back. If the email is
     * open next to the inbox, as it can be in a wide window, its screen is closed too.
     *
     * @param emailId the id of the email.
     */
    private fun delete(emailId: Long) {
        viewModelScope.launch {
            emails.delete(emailId)
            holler.show(AppMessage.EmailDeleted(emailId))

            if (EmailKey(emailId) in navigator.backStack) {
                navigator.popUpTo { it == InboxKey }
            }
        }
    }

    /** Checks for new emails, unless a check is already running. A failure becomes a message. */
    private fun refresh() {
        if (state.value.isRefreshing) return

        viewModelScope.launch {
            emails
                .refresh()
                .onFailure { holler.show(AppMessage.RefreshFailed) }
        }
    }
}
