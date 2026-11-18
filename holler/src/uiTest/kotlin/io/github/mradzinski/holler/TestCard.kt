package io.github.mradzinski.holler

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.mradzinski.holler.internal.CardPlace
import io.github.mradzinski.holler.internal.ControlDescriptions
import io.github.mradzinski.holler.internal.HollerCard
import io.github.mradzinski.holler.internal.defaultDuration
import io.github.mradzinski.holler.internal.startsExpanded
import io.github.mradzinski.holler.internal.textSlot
import kotlin.time.Duration

// A message is always drawn by a host, which adds a queue and animations around the card. The tests of
// the card itself (its timer, buttons, sizes, swipe) draw the library's internal card directly instead,
// so nothing but the card is on screen and its timing is not mixed with the host's.

/**
 * Draws one card with nothing around it, for the tests of the card itself.
 *
 * @param title the short message.
 * @param onDismiss called when the card should close, with the reason.
 * @param closeContentDescription content description of the close button.
 * @param expandContentDescription content description of the expand button while collapsed.
 * @param collapseContentDescription content description of the expand button while expanded.
 * @param modifier applied to the card.
 * @param details the long text, or `null`.
 * @param icon the icon, or `null`.
 * @param actions the actions, or `null`.
 * @param countdown draws the time left, or `null`.
 * @param actionsPosition where the actions are placed.
 * @param actionsVisibility how the presence of actions is signaled.
 * @param closePosition where the close button is placed, or that there is none.
 * @param showProgressBar whether the progress bar is drawn.
 * @param stoppableBy what the user can do to stop the timer for good.
 * @param duration the time on screen.
 * @param pauseOnPressOrHover whether pressing or hovering pauses the timer.
 * @param leaving whether the card is on its way out.
 * @param animateOnPress whether the card gives under a press.
 * @param colors the colors of the card.
 * @param style the style of the card.
 * @param expandIcon draws the symbol of the expand button.
 * @param closeIcon draws the symbol of the close button.
 */
@Composable
internal fun TestCard(
    title: @Composable () -> Unit,
    onDismiss: (HollerDismissReason) -> Unit,
    closeContentDescription: String = CLOSE_DESCRIPTION,
    expandContentDescription: String = EXPAND_DESCRIPTION,
    collapseContentDescription: String = COLLAPSE_DESCRIPTION,
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
    pauseOnPressOrHover: Boolean = false,
    leaving: Boolean = false,
    animateOnPress: Boolean = true,
    colors: HollerColors = HollerDefaults.colors(),
    style: HollerStyle = HollerDefaults.style(),
    expandIcon: @Composable (expanded: Boolean) -> Unit = { expanded -> HollerDefaults.ExpandIcon(expanded) },
    closeIcon: @Composable () -> Unit = { HollerDefaults.CloseIcon() },
) {
    HollerCard(
        title = title,
        onDismiss = onDismiss,
        modifier = modifier,
        contentModifier = Modifier,
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
        place = CardPlace(leaving = leaving, animateOnPress = animateOnPress),
        colors = colors,
        style = style,
        descriptions = ControlDescriptions(
            close = closeContentDescription,
            expand = expandContentDescription,
            collapse = collapseContentDescription,
        ),
        expandIcon = expandIcon,
        closeIcon = closeIcon,
    )
}

/**
 * Draws one card whose title and long text are plain text, for the tests of the card itself.
 *
 * @param title the short message.
 * @param onDismiss called when the card should close, with the reason.
 * @param closeContentDescription content description of the close button.
 * @param expandContentDescription content description of the expand button while collapsed.
 * @param collapseContentDescription content description of the expand button while expanded.
 * @param modifier applied to the card.
 * @param details the long text, or `null`.
 * @param icon the icon, or `null`.
 * @param actions the actions, or `null`.
 * @param countdown draws the time left, or `null`.
 * @param actionsPosition where the actions are placed.
 * @param actionsVisibility how the presence of actions is signaled.
 * @param closePosition where the close button is placed, or that there is none.
 * @param showProgressBar whether the progress bar is drawn.
 * @param stoppableBy what the user can do to stop the timer for good.
 * @param duration the time on screen.
 * @param pauseOnPressOrHover whether pressing or hovering pauses the timer.
 * @param leaving whether the card is on its way out.
 * @param animateOnPress whether the card gives under a press.
 * @param colors the colors of the card.
 * @param style the style of the card.
 * @param expandIcon draws the symbol of the expand button.
 * @param closeIcon draws the symbol of the close button.
 */
@Composable
internal fun TestCard(
    title: String,
    onDismiss: (HollerDismissReason) -> Unit,
    closeContentDescription: String = CLOSE_DESCRIPTION,
    expandContentDescription: String = EXPAND_DESCRIPTION,
    collapseContentDescription: String = COLLAPSE_DESCRIPTION,
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
    pauseOnPressOrHover: Boolean = false,
    leaving: Boolean = false,
    animateOnPress: Boolean = true,
    colors: HollerColors = HollerDefaults.colors(),
    style: HollerStyle = HollerDefaults.style(),
    expandIcon: @Composable (expanded: Boolean) -> Unit = { expanded -> HollerDefaults.ExpandIcon(expanded) },
    closeIcon: @Composable () -> Unit = { HollerDefaults.CloseIcon() },
): Unit = TestCard(
    title = { Text(title) },
    onDismiss = onDismiss,
    closeContentDescription = closeContentDescription,
    expandContentDescription = expandContentDescription,
    collapseContentDescription = collapseContentDescription,
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
    leaving = leaving,
    animateOnPress = animateOnPress,
    colors = colors,
    style = style,
    expandIcon = expandIcon,
    closeIcon = closeIcon,
)
