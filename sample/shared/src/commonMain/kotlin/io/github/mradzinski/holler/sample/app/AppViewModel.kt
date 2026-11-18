package io.github.mradzinski.holler.sample.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mradzinski.holler.Holler
import io.github.mradzinski.holler.HollerDismissReason
import io.github.mradzinski.holler.sample.core.navigation.InboxKey
import io.github.mradzinski.holler.sample.core.navigation.Navigator
import io.github.mradzinski.holler.sample.core.presentation.WhileScreenIsWatching
import io.github.mradzinski.holler.sample.data.connectivity.Connectivity
import io.github.mradzinski.holler.sample.data.email.EmailRepository
import io.github.mradzinski.holler.sample.data.email.ReplyStatus
import io.github.mradzinski.holler.sample.data.settings.SettingsRepository
import io.github.mradzinski.holler.sample.messages.AppMessage
import io.github.mradzinski.holler.sample.messages.OFFLINE_KEY
import io.github.mradzinski.holler.sample.messages.REPLY_KEY
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The view model of the app's root. It lives as long as the app is open, which makes it the place for
 * what no single screen owns: the actions of a message (Undo, Retry), which must keep working after
 * the screen that caused the message has closed, the messages about something no screen started (the
 * connection being lost, a reply on its way), and whether the app is offline.
 *
 * @param settings where the look of the app is read from.
 * @param connectivity tells whether the app is online, for the state and for the "You're offline" notice.
 * @param emails used by the actions of the messages, and to follow the last reply.
 * @param holler shows the messages. It is the same object the app's `HollerHost` draws.
 */
internal class AppViewModel(
    settings: SettingsRepository,
    connectivity: Connectivity,
    private val emails: EmailRepository,
    private val holler: Holler,
    private val navigator: Navigator,
) : ViewModel() {

    /** The state the app's root draws. */
    val state: StateFlow<AppUiState> = settings.settings
        .combine(connectivity.isOnline) { current, online ->
            AppUiState(
                darkTheme = current.darkTheme,
                brandLook = current.brandLook,
                messagesAtTop = current.messagesAtTop,
                messagesAsPile = current.messagesAsPile,
                offline = !online,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = WhileScreenIsWatching,
            initialValue = AppUiState(),
        )

    /** Whether "Sending reply…" was closed while its reply was still on its way. */
    private var replyNoticeClosed: Boolean = false

    init {
        connectivity.isOnline.onEach(::showConnection).launchIn(viewModelScope)
        emails.replyStatus.onEach(::showReply).launchIn(viewModelScope)
    }

    /**
     * Handles an event of the app's root.
     *
     * @param event the event.
     */
    fun onEvent(event: AppEvent) {
        when (event) {
            is AppEvent.OnUndoDeleteClick -> viewModelScope.launch { emails.restore(event.emailId) }
            is AppEvent.OnRetryRefreshClick -> retryRefresh()
        }
    }

    /**
     * Shows "You're offline" when the connection is lost. It is a short notice of the change, which
     * closes on its own; the state's `offline` is what tells the screens the app is still offline. The
     * message is shown with a key, which lets it be closed early if the connection comes back first.
     *
     * @param online whether the app is online.
     */
    private fun showConnection(online: Boolean) {
        if (online) {
            holler.dismiss(OFFLINE_KEY)
        } else {
            holler.show(AppMessage.Offline, key = OFFLINE_KEY)
        }
    }

    /**
     * Shows where the last reply stands. Every status uses the same key, so "Sending reply…" turns into
     * "Reply sent" or "Reply not sent" in the same card. Once the result is shown, the repository is told
     * it was seen.
     *
     * A user who closed "Sending reply…" has said they do not want to follow this reply, so "Reply sent"
     * is not shown to them. "Reply not sent" still is: a reply that was lost is something to know.
     *
     * @param status where the reply stands.
     */
    private fun showReply(status: ReplyStatus) {
        when (status) {
            ReplyStatus.Idle -> Unit

            ReplyStatus.Sending -> {
                showReplySending()
                navigator.popUpTo { it == InboxKey }
            }

            ReplyStatus.Sent -> {
                if (!replyNoticeClosed) holler.show(AppMessage.ReplySent, key = REPLY_KEY)

                emails.acknowledgeReply()
            }

            ReplyStatus.Failed -> {
                holler.show(AppMessage.ReplyFailed, key = REPLY_KEY)
                emails.acknowledgeReply()
            }
        }
    }

    /**
     * Shows "Sending reply…" and waits to learn how it ends. The wait runs in this view model's scope,
     * so Holler keeps nothing of it. When the result of the reply replaces the message, the wait
     * returns [HollerDismissReason.Replaced]; any other reason means the message was closed while the
     * reply was still on its way.
     */
    private fun showReplySending() {
        replyNoticeClosed = false

        val sending = holler.show(AppMessage.ReplySending, key = REPLY_KEY)

        viewModelScope.launch {
            val reason = sending.awaitDismiss()

            if (reason != HollerDismissReason.Replaced) replyNoticeClosed = true
        }
    }

    /** Checks for new emails again, and shows the same message once more if it fails again. */
    private fun retryRefresh() {
        viewModelScope.launch {
            emails
                .refresh()
                .onFailure { holler.show(AppMessage.RefreshFailed) }
        }
    }
}
