package io.github.mradzinski.holler

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.mradzinski.holler.internal.AnimatedCard
import io.github.mradzinski.holler.internal.CardPlace
import io.github.mradzinski.holler.internal.HollerCard
import io.github.mradzinski.holler.internal.defaultDuration
import io.github.mradzinski.holler.internal.hostedFrame
import io.github.mradzinski.holler.internal.startsExpanded
import io.github.mradzinski.holler.internal.textSlot
import kotlin.time.Duration

/**
 * Receiver of the `message` lambda of a [HollerHost], which gives a look to the messages that were shown as
 * values. It is what makes `HollerMessage(…)` available there, and only there: a message is always drawn by
 * a host.
 */
public sealed interface HollerHostScope

/** The one [HollerHostScope]. It holds nothing: what a host prepared for a message travels with the composition. */
internal object HostedScope : HollerHostScope

/**
 * Draws a message that was shown as a value, with fully custom content. Call it from the `message`
 * lambda of a [HollerHost], for the value that lambda received. The host does the rest: the queue, the place
 * among the other messages, the enter and exit animations and the accessibility descriptions. This is
 * the most flexible form; see the overload taking `String` for the common case
 * of plain text.
 *
 * The key of the message belongs to the `show(value, key)` call. To react to the message closing, the
 * code that showed it waits with [HollerHandle.awaitDismiss].
 *
 * Behavior worth knowing:
 * - Every message can be closed: it can always be swiped away sideways, and it has a close button
 *   unless [closePosition] says otherwise.
 * - Expanding the message stops its timer for good, unless [stoppableBy] says otherwise. The
 *   countdown and the progress bar then disappear and the message stays until closed.
 * - Dragging the message pauses the timer while the finger (or mouse button) is down. Letting go
 *   without dismissing resumes it from where it was; a stopped timer stays stopped.
 *
 * @param title the short message, always visible. A Material `Text` placed in it
 * uses the title text style and color by itself.
 * @param modifier modifier applied to the card.
 * @param details the long text, revealed by expanding the card. `null` (the default) means
 * the card cannot be expanded and has no expand button.
 * @param icon the icon shown before the title, supplied by the app: the library has no icons of its
 * own. A Material `Icon` without a tint is drawn in the title color. `null` (the default) shows no icon.
 * @param actions the call-to-action content, usually one or more Material buttons.
 * Its receiver lets an action close the message with [HollerActionsScope.dismiss]. `null` (the
 * default) shows no actions. With actions but no long text there is nothing to expand, so the
 * actions are always visible whatever [actionsVisibility] says.
 * @param countdown draws the time left before the message closes, in a strip at the bottom of
 * the card. It receives the remaining time rounded up to whole seconds and is called again once
 * per second; nothing else in the message recomposes for it. `null` (the default) shows no
 * countdown. Never shown for [HollerDuration.Infinite].
 * @param actionsPosition where the actions are placed: in the top row after the title, in the middle under
 * the title and the long text (the default), or in the strip at the bottom. See [HollerActionsPosition].
 * @param actionsVisibility how the message signals that it has actions when it also has a long
 * message: always visible (the default), by starting expanded, or by a hint on the expand
 * button. A message that starts expanded has no timer: see [duration]. It applies to actions in the middle
 * only. Ignored when there are no actions, and when there is no
 * long text (the actions are then always visible because there is nothing to expand).
 * @param closePosition where the close button is placed: at the end of the top row (the default) or of
 * the strip at the bottom, or [HollerClosePosition.None] for a message without one, which is closed
 * by swiping, by an action, by code or by its timer.
 * @param showProgressBar whether the progress bar along the bottom edge is drawn. Defaults to
 * `true`. It is never drawn for [HollerDuration.Infinite].
 * @param stoppableBy what the user can do to stop the timer for good: expand the message
 * ([HollerStopTrigger.Expand], the default) or nothing. See [HollerStopTrigger].
 * @param duration how long the message stays before closing itself. Defaults to
 * [HollerDuration.Short], or to [HollerDuration.Infinite] for a message that starts expanded. With
 * [HollerDuration.Infinite] it never closes by itself and no progress bar or countdown is shown. A
 * message that starts expanded must be infinite, unless [stoppableBy] is [HollerStopTrigger.None]:
 * expanding is what stops a timer, and a message that is already expanded could only stop it by being
 * collapsed and expanded again. Passing a finite duration to such a message fails.
 * @param pauseOnPressOrHover whether the timer is paused while the message is pressed (a finger or
 * a mouse button held down on it) or a mouse pointer hovers over it, and continues when it is let go
 * or the pointer leaves. Defaults to `true`. Hovering only happens where there is a mouse or a
 * trackpad. A pause is not a stop: a timer that was stopped stays stopped. Dragging the message
 * pauses its timer whatever this value is.
 * @param colors the colors of this message, or `null` (the default) to use the host's.
 * @param style the style of this message, or `null` (the default) to use the host's.
 * @param animation how this message enters and exits, or `null` (the default) to use the host's.
 */
@Composable
public fun HollerHostScope.HollerMessage(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    details: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    actions: (@Composable HollerActionsScope.() -> Unit)? = null,
    countdown: (@Composable (remaining: Duration) -> Unit)? = null,
    actionsPosition: HollerActionsPosition = HollerActionsPosition.Middle,
    actionsVisibility: HollerActionsVisibility = HollerActionsVisibility.AlwaysVisible,
    closePosition: HollerClosePosition = HollerClosePosition.Top,
    showProgressBar: Boolean = true,
    stoppableBy: HollerStopTrigger = HollerStopTrigger.Expand,
    duration: HollerDuration = defaultDuration(
        startsExpanded = startsExpanded(details != null, actions != null, actionsPosition, actionsVisibility),
        stoppableBy = stoppableBy,
    ),
    pauseOnPressOrHover: Boolean = true,
    colors: HollerColors? = null,
    style: HollerStyle? = null,
    animation: HollerAnimation? = null,
) {
    val frame = hostedFrame()
    val messageStyle = style ?: frame.style

    frame.AnimatedCard(
        animation = animation ?: frame.animation,
        style = messageStyle,
    ) { cardModifier, contentModifier ->
        HollerCard(
            title = title,
            onDismiss = frame.entry::dismiss,
            modifier = cardModifier.then(modifier),
            contentModifier = contentModifier,
            details = details,
            icon = icon,
            actions = actions,
            countdown = countdown,
            duration = duration,
            actionsPosition = actionsPosition,
            actionsVisibility = actionsVisibility,
            closePosition = closePosition,
            showProgressBar = showProgressBar,
            stoppableBy = stoppableBy,
            pauseOnPressOrHover = pauseOnPressOrHover,
            place = CardPlace(
                // Closing a message, whatever closes it, starts its exit by turning this target off.
                leaving = !frame.entry.visibility.targetState,
                animateOnPress = frame.animateOnPress,
                covered = frame.pile?.covered == true,
                pilePress = frame.pile?.press,
            ),
            colors = colors ?: frame.colors,
            style = messageStyle,
            descriptions = frame.descriptions,
            expandIcon = frame.expandIcon,
            closeIcon = frame.closeIcon,
        )
    }
}

/**
 * Draws a message that was shown as a value, whose title and long text are plain text. Call it
 * from the `message` lambda of a [HollerHost], for the value that lambda received.
 *
 * @param title the short message, always visible. It is not truncated: keep it short.
 * @param modifier modifier applied to the card.
 * @param details the long text, revealed by expanding the card. `null` (the default) means
 * the card cannot be expanded and has no expand button.
 * @param icon the icon shown before the title, supplied by the app: the library has no icons of its
 * own. A Material `Icon` without a tint is drawn in the title color. `null` (the default) shows no icon.
 * @param actions the call-to-action content, usually one or more Material buttons.
 * Its receiver lets an action close the message with [HollerActionsScope.dismiss]. `null` (the
 * default) shows no actions. With actions but no long text there is nothing to expand, so the
 * actions are always visible whatever [actionsVisibility] says.
 * @param countdown draws the time left before the message closes, in a strip at the bottom of
 * the card. It receives the remaining time rounded up to whole seconds and is called again once
 * per second; nothing else in the message recomposes for it. `null` (the default) shows no
 * countdown. Never shown for [HollerDuration.Infinite].
 * @param actionsPosition where the actions are placed: in the top row after the title, in the middle under
 * the title and the long text (the default), or in the strip at the bottom. See [HollerActionsPosition].
 * @param actionsVisibility how the message signals that it has actions when it also has a long
 * message: always visible (the default), by starting expanded, or by a hint on the expand
 * button. A message that starts expanded has no timer: see [duration]. It applies to actions in the middle
 * only. Ignored when there are no actions, and when there is no
 * long text (the actions are then always visible because there is nothing to expand).
 * @param closePosition where the close button is placed: at the end of the top row (the default) or of
 * the strip at the bottom, or [HollerClosePosition.None] for a message without one, which is closed
 * by swiping, by an action, by code or by its timer.
 * @param showProgressBar whether the progress bar along the bottom edge is drawn. Defaults to
 * `true`. It is never drawn for [HollerDuration.Infinite].
 * @param stoppableBy what the user can do to stop the timer for good: expand the message
 * ([HollerStopTrigger.Expand], the default) or nothing. See [HollerStopTrigger].
 * @param duration how long the message stays before closing itself. Defaults to
 * [HollerDuration.Short], or to [HollerDuration.Infinite] for a message that starts expanded. With
 * [HollerDuration.Infinite] it never closes by itself and no progress bar or countdown is shown. A
 * message that starts expanded must be infinite, unless [stoppableBy] is [HollerStopTrigger.None]:
 * expanding is what stops a timer, and a message that is already expanded could only stop it by being
 * collapsed and expanded again. Passing a finite duration to such a message fails.
 * @param pauseOnPressOrHover whether the timer is paused while the message is pressed (a finger or
 * a mouse button held down on it) or a mouse pointer hovers over it, and continues when it is let go
 * or the pointer leaves. Defaults to `true`. Hovering only happens where there is a mouse or a
 * trackpad. A pause is not a stop: a timer that was stopped stays stopped. Dragging the message
 * pauses its timer whatever this value is.
 * @param colors the colors of this message, or `null` (the default) to use the host's.
 * @param style the style of this message, or `null` (the default) to use the host's.
 * @param animation how this message enters and exits, or `null` (the default) to use the host's.
 */
@Composable
public fun HollerHostScope.HollerMessage(
    title: String,
    modifier: Modifier = Modifier,
    details: String? = null,
    icon: (@Composable () -> Unit)? = null,
    actions: (@Composable HollerActionsScope.() -> Unit)? = null,
    countdown: (@Composable (remaining: Duration) -> Unit)? = null,
    actionsPosition: HollerActionsPosition = HollerActionsPosition.Middle,
    actionsVisibility: HollerActionsVisibility = HollerActionsVisibility.AlwaysVisible,
    closePosition: HollerClosePosition = HollerClosePosition.Top,
    showProgressBar: Boolean = true,
    stoppableBy: HollerStopTrigger = HollerStopTrigger.Expand,
    duration: HollerDuration = defaultDuration(
        startsExpanded = startsExpanded(details != null, actions != null, actionsPosition, actionsVisibility),
        stoppableBy = stoppableBy,
    ),
    pauseOnPressOrHover: Boolean = true,
    colors: HollerColors? = null,
    style: HollerStyle? = null,
    animation: HollerAnimation? = null,
): Unit = HollerMessage(
    title = { Text(title) },
    modifier = modifier,
    details = textSlot(details),
    icon = icon,
    actions = actions,
    countdown = countdown,
    duration = duration,
    actionsPosition = actionsPosition,
    actionsVisibility = actionsVisibility,
    closePosition = closePosition,
    showProgressBar = showProgressBar,
    stoppableBy = stoppableBy,
    pauseOnPressOrHover = pauseOnPressOrHover,
    colors = colors,
    style = style,
    animation = animation,
)
