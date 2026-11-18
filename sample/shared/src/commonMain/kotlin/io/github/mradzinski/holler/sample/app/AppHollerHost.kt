package io.github.mradzinski.holler.sample.app

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mradzinski.holler.Holler
import io.github.mradzinski.holler.HollerActionsPosition
import io.github.mradzinski.holler.HollerAnimation
import io.github.mradzinski.holler.HollerClosePosition
import io.github.mradzinski.holler.HollerDuration
import io.github.mradzinski.holler.HollerHost
import io.github.mradzinski.holler.HollerHostScope
import io.github.mradzinski.holler.HollerMessage
import io.github.mradzinski.holler.HollerStackingStyle
import io.github.mradzinski.holler.HollerStopTrigger
import io.github.mradzinski.holler.sample.messages.AppMessage
import io.github.mradzinski.holler.sample.resources.Res
import io.github.mradzinski.holler.sample.resources.holler_close
import io.github.mradzinski.holler.sample.resources.holler_collapse
import io.github.mradzinski.holler.sample.resources.holler_expand
import io.github.mradzinski.holler.sample.resources.message_email_deleted
import io.github.mradzinski.holler.sample.resources.message_offline_title
import io.github.mradzinski.holler.sample.resources.message_refresh_failed_details
import io.github.mradzinski.holler.sample.resources.message_refresh_failed_title
import io.github.mradzinski.holler.sample.resources.message_reply_failed_details
import io.github.mradzinski.holler.sample.resources.message_reply_failed_title
import io.github.mradzinski.holler.sample.resources.message_reply_sending
import io.github.mradzinski.holler.sample.resources.message_reply_sent
import io.github.mradzinski.holler.sample.resources.message_retry
import io.github.mradzinski.holler.sample.resources.message_undo
import io.github.mradzinski.holler.sample.ui.component.MessageAction
import io.github.mradzinski.holler.sample.ui.component.MessageCountdown
import io.github.mradzinski.holler.sample.ui.playground.DemoMessage
import io.github.mradzinski.holler.sample.ui.playground.DemoMessageContent
import io.github.mradzinski.holler.sample.ui.theme.ErrorIcon
import io.github.mradzinski.holler.sample.ui.theme.MessageTheme
import io.github.mradzinski.holler.sample.ui.theme.SuccessIcon
import io.github.mradzinski.holler.sample.ui.theme.WarningIcon
import io.github.mradzinski.holler.sample.ui.theme.messageColors
import io.github.mradzinski.holler.sample.ui.theme.messageStyle
import kotlin.time.Duration.Companion.seconds
import org.jetbrains.compose.resources.stringResource

/*
 The app's one HollerHost, and the one place that knows how a message looks.

 View models show a message as plain data: `holler.show(AppMessage.EmailDeleted(id))`. The host's
 content below receives that data and gives it a title, an icon, actions and a duration. So a view
 model never touches anything visual, and a unit test can check what it showed by reading
 `holler.messages`.

 The playground's messages come here too. They are values like any other, and the playground keeps
 their looks next to its buttons, in `DemoMessageContent`.
*/

/**
 * Draws the app's messages over its screens.
 *
 * @param holler the host state, the same object the view models show their messages through.
 * @param state the state of the app's root, which says where messages go and which look they take.
 * @param onEvent called when an action of a message is pressed.
 * @param modifier applied to the host.
 */
@Composable
internal fun AppHollerHost(
    holler: Holler,
    state: AppUiState,
    onEvent: (AppEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = if (state.brandLook) {
        MessageTheme.Brand
    } else {
        MessageTheme.App
    }

    HollerHost(
        holler = holler,
        // Holler ships no text of its own: the app says what a screen reader announces for the buttons.
        closeContentDescription = stringResource(Res.string.holler_close),
        expandContentDescription = stringResource(Res.string.holler_expand),
        collapseContentDescription = stringResource(Res.string.holler_collapse),
        alignment = if (state.messagesAtTop) {
            Alignment.TopCenter
        } else {
            Alignment.BottomCenter
        },
        stackingStyle = if (state.messagesAsPile) {
            HollerStackingStyle.Pile
        } else {
            HollerStackingStyle.Column
        },
        colors = messageColors(theme),
        style = messageStyle(theme),
        modifier = modifier,
    ) { value ->
        // Everything the app shows as a value is one of these two; anything else is left undrawn and dropped.
        when (value) {
            is AppMessage -> AppMessageContent(message = value, onEvent = onEvent)
            is DemoMessage -> DemoMessageContent(message = value)
        }
    }
}

/**
 * Gives one of the app's messages its look. It is an extension of the host's scope because `HollerMessage(…)`
 * only exists there: a message is always drawn by a host.
 *
 * @param message the message to draw.
 * @param onEvent called when an action of the message is pressed.
 */
@Composable
private fun HollerHostScope.AppMessageContent(message: AppMessage, onEvent: (AppEvent) -> Unit) {
    when (message) {
        is AppMessage.EmailDeleted -> HollerMessage(
            title = stringResource(Res.string.message_email_deleted),
            icon = { SuccessIcon() },
            actions = {
                MessageAction(Res.string.message_undo) {
                    onEvent(AppEvent.OnUndoDeleteClick(message.emailId))
                    dismiss()
                }
            },
            countdown = { MessageCountdown(it) },
            duration = HollerDuration.Long,
            animation = HollerAnimation.Drop,
            closePosition = HollerClosePosition.None,
            actionsPosition = HollerActionsPosition.Top,
        )

        is AppMessage.RefreshFailed -> HollerMessage(
            title = stringResource(Res.string.message_refresh_failed_title),
            details = stringResource(Res.string.message_refresh_failed_details),
            icon = { ErrorIcon() },
            actions = {
                MessageAction(Res.string.message_retry) {
                    onEvent(AppEvent.OnRetryRefreshClick)
                    dismiss()
                }
            },
            duration = HollerDuration.Long,
        )

        is AppMessage.Offline -> HollerMessage(
            title = stringResource(Res.string.message_offline_title),
            icon = { WarningIcon() },
            duration = HollerDuration.Long,
            // A notice of a change, with nothing to act on: there is no reason to keep it on screen.
            stoppableBy = HollerStopTrigger.None,
            closePosition = HollerClosePosition.None,
        )

        is AppMessage.ReplySending -> HollerMessage(
            title = stringResource(Res.string.message_reply_sending),
            icon = {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            },
            duration = HollerDuration.Infinite,
        )

        is AppMessage.ReplySent -> HollerMessage(
            title = stringResource(Res.string.message_reply_sent),
            icon = { SuccessIcon() },
            showProgressBar = false,
            stoppableBy = HollerStopTrigger.None,
            duration = HollerDuration.Custom(2.seconds),
        )

        is AppMessage.ReplyFailed -> HollerMessage(
            title = stringResource(Res.string.message_reply_failed_title),
            details = stringResource(Res.string.message_reply_failed_details),
            icon = { ErrorIcon() },
            duration = HollerDuration.Long,
        )
    }
}
