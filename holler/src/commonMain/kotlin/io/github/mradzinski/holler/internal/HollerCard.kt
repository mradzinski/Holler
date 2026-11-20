package io.github.mradzinski.holler.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mradzinski.holler.HollerActionsPosition
import io.github.mradzinski.holler.HollerActionsScope
import io.github.mradzinski.holler.HollerActionsVisibility
import io.github.mradzinski.holler.HollerClosePosition
import io.github.mradzinski.holler.HollerColors
import io.github.mradzinski.holler.HollerDismissReason
import io.github.mradzinski.holler.HollerDuration
import io.github.mradzinski.holler.HollerStopTrigger
import io.github.mradzinski.holler.HollerStyle
import io.github.mradzinski.holler.ProvidableHollerColors
import kotlin.time.Duration

/** Padding between the card edge and its content, on the start and end sides. */
internal val EdgePadding: Dp = 12.dp

/** Space between the icon and the title. */
internal val IconGap: Dp = 11.dp

/** Length of the expand and collapse animation, in milliseconds. */
internal const val EXPAND_MILLIS: Int = 300

/** Length of the footer's collapse after the timer is stopped, in milliseconds. */
internal const val FOOTER_COLLAPSE_MILLIS: Int = 200

/**
 * The message card itself: layout, timer, expansion, close and swipe. The public
 * `HollerMessage` composables draw messages through this one function.
 *
 * @param title the short message, always visible.
 * @param onDismiss called when the message should be closed, with the reason. The caller is
 * responsible for actually removing the message.
 * @param modifier modifier applied to the card.
 * @param contentModifier modifier applied to what is inside the card, used by the host to
 * animate the content separately from the card.
 * @param details the long text. `null` means the card cannot be expanded.
 * @param icon the icon shown before the title, or `null` for none.
 * @param actions the call-to-action content, or `null` for none.
 * @param countdown draws the time left, or `null` for no countdown.
 * @param duration the time on screen. It must be infinite for a card that starts expanded, unless
 * nothing stops its timer: see [requireNoTimerToStop].
 * @param actionsPosition where the actions are placed: in the top row, in the middle under the title and
 * the long text, or in the strip at the bottom.
 * @param actionsVisibility how the presence of actions in the middle is signaled. Ignored when there is no
 * long text, because then the actions are always visible, and when the actions are not in the middle.
 * @param closePosition where the close button is placed, or that there is none.
 * @param showProgressBar whether the progress bar is drawn (never for an infinite duration).
 * @param stoppableBy what the user can do to stop the timer for good: whether expanding the
 * message stops it.
 * @param pauseOnPressOrHover whether the timer is paused while the card is pressed or a mouse
 * pointer hovers over it. Dragging the card pauses the timer whatever this value is.
 * @param place where the card stands among the messages: whether it is leaving, whether it gives
 * under a press, and whether it is behind another one in a pile.
 * @param state what the card keeps while its message shows the same content: its timer and whether it
 * is expanded. A card drawn again for the same content goes on from it.
 * @param colors the colors of the card.
 * @param style the shape, sizes and text styles of the card.
 * @param descriptions what a screen reader announces for the card's own buttons.
 * @param expandIcon draws the symbol of the expand button. It receives whether the message is expanded.
 * @param closeIcon draws the symbol of the close button.
 */
@Composable
internal fun HollerCard(
    title: @Composable () -> Unit,
    onDismiss: (HollerDismissReason) -> Unit,
    modifier: Modifier,
    contentModifier: Modifier,
    details: (@Composable () -> Unit)?,
    icon: (@Composable () -> Unit)?,
    actions: (@Composable HollerActionsScope.() -> Unit)?,
    countdown: (@Composable (remaining: Duration) -> Unit)?,
    duration: HollerDuration,
    actionsPosition: HollerActionsPosition,
    actionsVisibility: HollerActionsVisibility,
    closePosition: HollerClosePosition,
    showProgressBar: Boolean,
    stoppableBy: HollerStopTrigger,
    pauseOnPressOrHover: Boolean,
    place: CardPlace,
    state: CardState,
    colors: HollerColors,
    style: HollerStyle,
    descriptions: ControlDescriptions,
    expandIcon: @Composable (expanded: Boolean) -> Unit,
    closeIcon: @Composable () -> Unit,
) {
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val timer = remember(state, duration) { state.timerFor(duration) }

    val expandable = details != null
    val middleActions = actions.takeIf { actionsPosition == HollerActionsPosition.Middle }

    val actionsInBody = expandable && middleActions != null && actionsVisibility != HollerActionsVisibility.AlwaysVisible
    val startsExpanded = startsExpanded(expandable, actions != null, actionsPosition, actionsVisibility)

    requireNoTimerToStop(startsExpanded, stoppableBy, duration)

    val expansion = remember(state) { state.expansionFor(startsExpanded) }

    val showHint = actionsInBody && actionsVisibility == HollerActionsVisibility.ChevronHint && !expansion.hintSeen

    val messageScope = remember { ActionsScope { currentOnDismiss(HollerDismissReason.Action) } }

    RunTimer(timer, frameDriven = showProgressBar, tickWholeSeconds = countdown != null) {
        currentOnDismiss(HollerDismissReason.Timeout)
    }

    val gestures = rememberCardGestures(pauseOnPressOrHover, place) { currentOnDismiss(HollerDismissReason.Swipe) }
    val onClose = { currentOnDismiss(HollerDismissReason.CloseButton) }

    PauseTimerWhile(timer, held = gestures.holdsTimer)

    // The title color is the content color of the whole card, as on a Material surface, so an icon or
    // any other content that sets no color of its own is readable on the card.
    CompositionLocalProvider(ProvidableHollerColors provides colors, LocalContentColor provides colors.title) {
        Box(
            modifier = modifier.cardFrame(
                colors = colors,
                style = style,
                gestures = gestures,
                onDismissAction = { currentOnDismiss(HollerDismissReason.Swipe) },
            ),
        ) {
            Column(contentModifier.fillMaxWidth()) {
                CardHeader(
                    title = title,
                    icon = icon,
                    actions = actions.takeIf { actionsPosition == HollerActionsPosition.Top },
                    scope = messageScope,
                    expandable = expandable,
                    expanded = expansion.isExpanded,
                    showHint = showHint,
                    nudgeHint = showHint && !expansion.hintNudged,
                    showClose = closePosition == HollerClosePosition.Top,
                    colors = colors,
                    style = style,
                    descriptions = descriptions,
                    expandIcon = expandIcon,
                    closeIcon = closeIcon,
                    onToggleExpanded = {
                        if (expansion.toggle() && stoppableBy == HollerStopTrigger.Expand) timer.stop()
                    },
                    onHintNudged = { expansion.hintNudged = true },
                    onClose = onClose,
                )
                CardBody(
                    details = details,
                    actions = middleActions,
                    scope = messageScope,
                    expanded = expansion.isExpanded,
                    actionsInBody = actionsInBody,
                    startPadding = bodyStartPadding(hasIcon = icon != null, style = style),
                    colors = colors,
                    style = style,
                )
                CardFooter(
                    timer = timer,
                    countdown = countdown,
                    actions = actions.takeIf { actionsPosition == HollerActionsPosition.Bottom },
                    scope = messageScope,
                    showClose = closePosition == HollerClosePosition.Bottom,
                    showProgressBar = showProgressBar,
                    colors = colors,
                    style = style,
                    closeContentDescription = descriptions.close,
                    closeIcon = closeIcon,
                    onClose = onClose,
                )
            }
        }
    }
}

/**
 * Whether a card opens with its long text revealed: it has a long text and actions in the middle, and those
 * actions ask for it.
 *
 * @param hasDetails whether the card has a long text.
 * @param hasActions whether the card has actions.
 * @param actionsPosition where the actions are placed.
 * @param actionsVisibility how the presence of actions in the middle is signaled.
 * @return `true` when the card starts expanded.
 */
internal fun startsExpanded(
    hasDetails: Boolean,
    hasActions: Boolean,
    actionsPosition: HollerActionsPosition,
    actionsVisibility: HollerActionsVisibility,
): Boolean = hasDetails &&
    hasActions &&
    actionsPosition == HollerActionsPosition.Middle &&
    actionsVisibility == HollerActionsVisibility.StartExpanded

/**
 * The time on screen a message gets when it sets none: no limit for one that starts expanded and whose
 * timer expanding would stop, because that timer could never be stopped; the short time otherwise.
 *
 * @param startsExpanded whether the card starts expanded.
 * @param stoppableBy what the user can do to stop the timer for good.
 * @return the default duration.
 */
internal fun defaultDuration(startsExpanded: Boolean, stoppableBy: HollerStopTrigger): HollerDuration =
    if (startsExpanded && stoppableBy == HollerStopTrigger.Expand) {
        HollerDuration.Infinite
    } else {
        HollerDuration.Short
    }

/**
 * Refuses a card that starts expanded with a timer that expanding is meant to stop. The user could
 * only stop that timer by collapsing the card and expanding it again, so such a card must have no timer.
 *
 * @param startsExpanded whether the card starts expanded.
 * @param stoppableBy what the user can do to stop the timer for good.
 * @param duration the time on screen.
 */
internal fun requireNoTimerToStop(
    startsExpanded: Boolean,
    stoppableBy: HollerStopTrigger,
    duration: HollerDuration,
) {
    require(!startsExpanded || stoppableBy == HollerStopTrigger.None || duration == HollerDuration.Infinite) {
        "A message that starts expanded must use HollerDuration.Infinite: its timer could only be stopped by " +
            "collapsing and expanding it again. Pass duration = HollerDuration.Infinite, or " +
            "stoppableBy = HollerStopTrigger.None if it should close on time whatever the user does."
    }
}

/**
 * The receiver given to the actions of a card.
 *
 * @param onDismiss called when an action closes the message.
 */
private class ActionsScope(private val onDismiss: () -> Unit) : HollerActionsScope {

    /** Closes the message, reporting [HollerDismissReason.Action]. */
    override fun dismiss() = onDismiss()
}

/**
 * Keeps the timer counting for as long as the card is on screen, and reports once when its time
 * has run out. It restarts the counting whenever the timer is stopped, paused or resumed, or the
 * way it should count changes; a pause cuts the current wait short and a resume starts a new one.
 *
 * @param timer the timer of the message.
 * @param frameDriven whether the timer advances on every frame, for a visible progress bar.
 * @param tickWholeSeconds whether the timer advances once per second, for a countdown.
 * @param onTimeout called once when the full time has elapsed.
 */
@Composable
private fun RunTimer(
    timer: HollerTimer,
    frameDriven: Boolean,
    tickWholeSeconds: Boolean,
    onTimeout: () -> Unit,
) {
    val currentOnTimeout by rememberUpdatedState(onTimeout)
    var timeoutReported by remember(timer) { mutableStateOf(false) }

    LaunchedEffect(
        timer,
        timer.isStopped,
        timer.isPaused,
        frameDriven,
        tickWholeSeconds,
    ) {
        timer.run(frameDriven = frameDriven, tickWholeSeconds = tickWholeSeconds)

        if (timer.isFinished && !timeoutReported) {
            timeoutReported = true
            currentOnTimeout()
        }
    }
}

/**
 * Pauses the timer for as long as the card is held, by a drag, a press or a hovering pointer, and
 * lets it continue when it no longer is. Several of these can overlap, which is why the timer
 * follows one combined value: it continues only once none of them holds the card.
 *
 * @param timer the timer of the message.
 * @param held whether anything is holding the card at the moment.
 */
@Composable
private fun PauseTimerWhile(timer: HollerTimer, held: Boolean) {
    LaunchedEffect(timer, held) {
        if (held) {
            timer.pause()
        } else {
            timer.resume()
        }
    }
}

/**
 * Everything around the content of the card: the room for its shadow, the swipe, the give under a
 * press, its width, the press and hover detection, its surface, and what it tells assistive technology.
 *
 * @param colors the colors of the card.
 * @param style the shape, sizes and shadow geometry of the card.
 * @param gestures the swipe, the press and the hover of the card.
 * @param onDismissAction called when assistive technology asks to dismiss the card.
 * @return this modifier followed by the frame of the card.
 */
private fun Modifier.cardFrame(
    colors: HollerColors,
    style: HollerStyle,
    gestures: CardGestures,
    onDismissAction: () -> Unit,
): Modifier = this
    // The swipe layer and the press layer sit between the bleed and its padding, so they are large
    // enough to hold the shadow while the card fades or changes size. See [shadowBleed].
    .shadowBleed(style.shadowExtent)
    .then(gestures.swipe.layer)
    .graphicsLayer {
        scaleX = gestures.scale()
        scaleY = gestures.scale()
    }
    .padding(style.shadowExtent)
    .widthIn(max = style.maxWidth)
    .fillMaxWidth()
    .then(gestures.swipe.gesture)
    .then(gestures.pressOrHover.modifier)
    .cardSurface(colors, style)
    .semantics {
        liveRegion = LiveRegionMode.Polite

        // What swiping does, for someone who cannot swipe: assistive technology offers it as its own
        // way of dismissing, so the message can be closed without a close button.
        dismiss {
            onDismissAction()

            true
        }
    }

/**
 * Draws the card's surface: its shadow, shape, background and outline. The shadow is drawn by the
 * library and not by the platform, so it looks the same everywhere.
 *
 * @param colors the colors of the card.
 * @param style the shape and shadow geometry of the card.
 * @return this modifier followed by the surface drawing.
 */
private fun Modifier.cardSurface(colors: HollerColors, style: HollerStyle): Modifier = this
    .dropShadow(
        style.shape,
        Shadow(
            radius = style.shadowRadius,
            color = colors.shadow,
            spread = style.shadowSpread,
            offset = style.shadowOffset,
        ),
    )
    .clip(style.shape)
    .background(colors.container)
    .border(1.dp, colors.border, style.shape)
