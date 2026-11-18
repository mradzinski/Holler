package io.github.mradzinski.holler.sample.ui.playground

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mradzinski.holler.Holler
import io.github.mradzinski.holler.HollerActionsPosition
import io.github.mradzinski.holler.HollerActionsVisibility
import io.github.mradzinski.holler.HollerAnimation
import io.github.mradzinski.holler.HollerClosePosition
import io.github.mradzinski.holler.HollerDuration
import io.github.mradzinski.holler.HollerHostScope
import io.github.mradzinski.holler.HollerIconRing
import io.github.mradzinski.holler.HollerMessage
import io.github.mradzinski.holler.LocalHollerColors
import io.github.mradzinski.holler.sample.resources.Res
import io.github.mradzinski.holler.sample.resources.backup_finished_message
import io.github.mradzinski.holler.sample.resources.backup_finished_title
import io.github.mradzinski.holler.sample.resources.changes_saved_message
import io.github.mradzinski.holler.sample.resources.changes_saved_okay
import io.github.mradzinski.holler.sample.resources.changes_saved_title
import io.github.mradzinski.holler.sample.resources.connection_lost_ignore
import io.github.mradzinski.holler.sample.resources.connection_lost_message
import io.github.mradzinski.holler.sample.resources.connection_lost_retry
import io.github.mradzinski.holler.sample.resources.connection_lost_title
import io.github.mradzinski.holler.sample.resources.conversation_archived_title
import io.github.mradzinski.holler.sample.resources.draft_saved_title
import io.github.mradzinski.holler.sample.resources.focus_mode_message
import io.github.mradzinski.holler.sample.resources.focus_mode_title
import io.github.mradzinski.holler.sample.resources.focus_mode_turn_off
import io.github.mradzinski.holler.sample.resources.invitation_accept
import io.github.mradzinski.holler.sample.resources.invitation_decline
import io.github.mradzinski.holler.sample.resources.invitation_message
import io.github.mradzinski.holler.sample.resources.invitation_title
import io.github.mradzinski.holler.sample.resources.link_copied_title
import io.github.mradzinski.holler.sample.resources.message_undo
import io.github.mradzinski.holler.sample.resources.storage_full_free_up
import io.github.mradzinski.holler.sample.resources.storage_full_title
import io.github.mradzinski.holler.sample.resources.swipe_away_title
import io.github.mradzinski.holler.sample.resources.update_available_install
import io.github.mradzinski.holler.sample.resources.update_available_message
import io.github.mradzinski.holler.sample.resources.update_available_title
import io.github.mradzinski.holler.sample.resources.upload_finished_title
import io.github.mradzinski.holler.sample.resources.upload_started_title
import io.github.mradzinski.holler.sample.ui.component.MessageAction
import io.github.mradzinski.holler.sample.ui.component.MessageCountdown
import io.github.mradzinski.holler.sample.ui.theme.ErrorIcon
import io.github.mradzinski.holler.sample.ui.theme.InfoIcon
import io.github.mradzinski.holler.sample.ui.theme.SuccessIcon
import io.github.mradzinski.holler.sample.ui.theme.WarningIcon
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

// The look of each message of the playground, and the one demo that needs more than a `show` call.

/** The key shared by the two messages of the upload demo, which makes the second replace the first. */
private const val UPLOAD_KEY: String = "upload"

/** How long the pretend upload of the demo takes. */
private val UploadTime: Duration = 3.seconds

/**
 * Shows a message that is replaced by another: "Uploading photo…" stays until a pretend upload ends,
 * then "Photo uploaded" takes its place in the same card and closes by itself. Both are shown with
 * the same key, which is what makes the second replace the first; pressing the button again
 * replaces whichever of the two is on screen, so the messages never pile up.
 *
 * @param scope the coroutine scope the pretend upload, and the wait for its message to close, run in.
 */
internal fun Holler.showUpload(scope: CoroutineScope) {
    val upload = scope.launch {
        delay(UploadTime)
        show(DemoMessage.Uploaded, key = UPLOAD_KEY)
    }

    val uploading = show(DemoMessage.Uploading, key = UPLOAD_KEY)

    // Whatever takes this message away ends its upload: the user closing it, or another press of the
    // button replacing it. When the upload itself replaces it, there is nothing left to end. The wait
    // runs in the screen's scope, so it ends with the screen and Holler keeps nothing of it.
    scope.launch {
        uploading.awaitDismiss()
        upload.cancel()
    }
}

/**
 * Gives one of the playground's messages its look. It is an extension of the host's scope because
 * `HollerMessage(…)` only exists there; the app's host calls it for every [DemoMessage] it receives.
 *
 * @param message the message to draw.
 */
@Composable
internal fun HollerHostScope.DemoMessageContent(message: DemoMessage) {
    when (message) {
        // Every part at once: icon, title, long text, one action hinted on the expand button, a countdown
        // and a progress bar, for fifteen seconds.
        is DemoMessage.Everything -> HollerMessage(
            title = stringResource(Res.string.changes_saved_title),
            details = stringResource(Res.string.changes_saved_message),
            icon = { SuccessIcon() },
            actions = { MessageAction(Res.string.changes_saved_okay) { dismiss() } },
            countdown = { MessageCountdown(it) },
            duration = HollerDuration.Custom(15.seconds),
            actionsVisibility = HollerActionsVisibility.ChevronHint,
        )

        is DemoMessage.TitleOnly -> HollerMessage(
            title = stringResource(Res.string.link_copied_title),
            icon = { InfoIcon() },
        )

        is DemoMessage.ActionsAlwaysVisible -> HollerMessage(
            title = stringResource(Res.string.connection_lost_title),
            details = stringResource(Res.string.connection_lost_message),
            icon = { ErrorIcon() },
            actions = {
                MessageAction(Res.string.connection_lost_retry) { dismiss() }
                MessageAction(Res.string.connection_lost_ignore) { dismiss() }
            },
            duration = HollerDuration.Long,
        )

        // A message that starts expanded has no timer, so it sets no duration and no countdown: it
        // stays until the user acts on it or closes it.
        is DemoMessage.StartsExpanded -> HollerMessage(
            title = stringResource(Res.string.update_available_title),
            details = stringResource(Res.string.update_available_message),
            icon = { InfoIcon() },
            actions = { MessageAction(Res.string.update_available_install) { dismiss() } },
            actionsVisibility = HollerActionsVisibility.StartExpanded,
        )

        // The user has to act on it or close it.
        is DemoMessage.Infinite -> HollerMessage(
            title = stringResource(Res.string.storage_full_title),
            icon = { WarningIcon() },
            actions = { MessageAction(Res.string.storage_full_free_up) { dismiss() } },
            duration = HollerDuration.Infinite,
        )

        is DemoMessage.WithoutProgressBar -> HollerMessage(
            title = stringResource(Res.string.draft_saved_title),
            icon = { SuccessIcon() },
            countdown = { MessageCountdown(it) },
            showProgressBar = false,
            // One message can enter and exit its own way, whatever the host's animation is.
            animation = HollerAnimation.Fade,
        )

        is DemoMessage.Position -> PositionDemoContent(message)

        is DemoMessage.CustomLook -> CustomLookDemoContent()

        is DemoMessage.Uploading -> HollerMessage(
            title = stringResource(Res.string.upload_started_title),
            icon = {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            },
            duration = HollerDuration.Infinite,
        )

        is DemoMessage.Uploaded -> HollerMessage(
            title = stringResource(Res.string.upload_finished_title),
            icon = { SuccessIcon() },
        )
    }
}

/**
 * Draws the message that has colors and a style of its own. The two are passed to the message, so
 * they win over the host's for this message only. The icon and the action read the message's colors
 * from `LocalHollerColors`, so they follow whatever colors the message is given.
 */
@Composable
private fun HollerHostScope.CustomLookDemoContent() {
    HollerMessage(
        title = stringResource(Res.string.focus_mode_title),
        details = stringResource(Res.string.focus_mode_message),
        icon = {
            HollerIconRing(tint = LocalHollerColors.current.progress) {
                Icon(Icons.Default.Notifications, contentDescription = null)
            }
        },
        actions = { MessageAction(Res.string.focus_mode_turn_off) { dismiss() } },
        countdown = { MessageCountdown(it) },
        duration = HollerDuration.Long,
        colors = demoColors(),
        style = demoStyle(),
    )
}

/**
 * Gives a look to the messages that show where the actions and the close button can be placed.
 *
 * @param message the message to draw.
 */
@Composable
private fun HollerHostScope.PositionDemoContent(message: DemoMessage.Position) {
    when (message) {
        // One short action next to the title, as on a snackbar.
        is DemoMessage.Position.ActionAtTop -> HollerMessage(
            title = stringResource(Res.string.conversation_archived_title),
            icon = { SuccessIcon() },
            actions = { MessageAction(Res.string.message_undo) { dismiss() } },
            duration = HollerDuration.Long,
            actionsPosition = HollerActionsPosition.Top,
        )

        // The actions share the strip with the countdown. Expanding stops the timer: the countdown
        // leaves and the actions stay.
        is DemoMessage.Position.ActionsAtBottom -> HollerMessage(
            title = stringResource(Res.string.invitation_title),
            details = stringResource(Res.string.invitation_message),
            icon = { InfoIcon() },
            actions = {
                MessageAction(Res.string.invitation_accept) { dismiss() }
                MessageAction(Res.string.invitation_decline) { dismiss() }
            },
            countdown = { MessageCountdown(it) },
            duration = HollerDuration.Long,
            actionsPosition = HollerActionsPosition.Bottom,
        )

        is DemoMessage.Position.CloseAtBottom -> HollerMessage(
            title = stringResource(Res.string.backup_finished_title),
            details = stringResource(Res.string.backup_finished_message),
            icon = { SuccessIcon() },
            countdown = { MessageCountdown(it) },
            duration = HollerDuration.Long,
            closePosition = HollerClosePosition.Bottom,
        )

        is DemoMessage.Position.WithoutCloseButton -> HollerMessage(
            title = stringResource(Res.string.swipe_away_title),
            icon = { InfoIcon() },
            countdown = { MessageCountdown(it) },
            duration = HollerDuration.Long,
            closePosition = HollerClosePosition.None,
        )
    }
}
