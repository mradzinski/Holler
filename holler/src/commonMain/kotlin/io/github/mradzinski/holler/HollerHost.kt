package io.github.mradzinski.holler

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mradzinski.holler.internal.ControlDescriptions
import io.github.mradzinski.holler.internal.DrawnMark
import io.github.mradzinski.holler.internal.HollerEntry
import io.github.mradzinski.holler.internal.HostedFrame
import io.github.mradzinski.holler.internal.HostedValue
import io.github.mradzinski.holler.internal.Pile
import io.github.mradzinski.holler.internal.PilePlace

/**
 * Draws the messages of [holler] on top of your screen. Place it last inside a full-size `Box`
 * that also holds your content, so messages appear above it. The host itself takes no touches:
 * everything under it stays usable.
 *
 * @param holler the object whose messages are drawn: the one the app calls `show` on.
 * @param closeContentDescription what a screen reader announces for the close button, for example
 * "Close". These three descriptions are required and have no defaults: the library ships no text, so
 * the app supplies them in its own language.
 * @param expandContentDescription what a screen reader announces for the expand button while the
 * message is collapsed, for example "Show more".
 * @param collapseContentDescription what a screen reader announces for the expand button while the
 * message is expanded, for example "Show less".
 * @param modifier modifier applied to the host, which fills the space it is given.
 * @param alignment where messages are placed. Defaults to the bottom center.
 * @param stackingStyle how the messages shown at the same time are arranged: one under the other
 * ([HollerStackingStyle.Column], the default) or as a pile with the newest in front
 * ([HollerStackingStyle.Pile]). In a pile only the front message is visible as a message, so only its
 * timer runs: one that gets covered keeps the time it had left.
 * @param maxVisible how many messages may be on screen at once. Defaults to 3 one under the other,
 * where the rest wait their turn, and to 5 in a pile, where it is the number of cards in the pile and
 * the earliest messages beyond it are not drawn until there is room. Must be at least 1.
 * @param contentPadding space kept between the messages and the edges of the host, in addition
 * to the system bars and display cutouts, which are always avoided.
 * @param spacing space between messages that are one under the other. It has no effect in a pile.
 * @param colors the colors of every message that does not set its own. Defaults to
 * [HollerDefaults.colors], which takes them from the app's Material theme.
 * @param style the style of every message that does not set its own.
 * @param animation how messages enter and exit, unless a message sets its own. Defaults to
 * [HollerAnimation.Default].
 * @param animateOnPress whether a message gives under a press: drawn slightly smaller while a finger
 * or a mouse button is held down on it, and returning to its size with a small bounce when let go.
 * Defaults to `true`. It is set for the whole host; a message cannot change it.
 * @param expandIcon draws the symbol of the expand button of every message, inside the button the
 * library provides. It receives whether the message is expanded, so it can turn, change or stay as it
 * is. Defaults to [HollerDefaults.ExpandIcon], a chevron that turns. A Material `Icon` without a tint
 * is drawn in [HollerColors.controls]. Pass `contentDescription = null` to your icon: the button
 * already announces [expandContentDescription] or [collapseContentDescription].
 * @param closeIcon draws the symbol of the close button of every message, inside the button the
 * library provides. Defaults to [HollerDefaults.CloseIcon], a cross. Pass `contentDescription = null`
 * to your icon: the button already announces [closeContentDescription].
 * @param message draws a message that was shown as a value, with `show(value)`. It receives the value
 * and gives it a look by calling `HollerMessage(…)`, usually from a `when` over
 * the app's own message types. A message it draws nothing for is dropped. The default draws nothing,
 * which suits an app that only shows text messages, with `show(title, …)`: the host draws those itself.
 */
@Composable
public fun HollerHost(
    holler: Holler,
    closeContentDescription: String,
    expandContentDescription: String,
    collapseContentDescription: String,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.BottomCenter,
    stackingStyle: HollerStackingStyle = HollerStackingStyle.Column,
    maxVisible: Int = HollerDefaults.maxVisible(stackingStyle),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    spacing: Dp = 12.dp,
    colors: HollerColors = HollerDefaults.colors(),
    style: HollerStyle = HollerDefaults.style(),
    animation: HollerAnimation = HollerAnimation.Default,
    animateOnPress: Boolean = true,
    expandIcon: @Composable (expanded: Boolean) -> Unit = { expanded -> HollerDefaults.ExpandIcon(expanded) },
    closeIcon: @Composable () -> Unit = { HollerDefaults.CloseIcon() },
    message: @Composable HollerHostScope.(value: Any) -> Unit = {},
) {
    require(maxVisible >= 1) { "maxVisible must be at least 1, was $maxVisible" }

    val layoutDirection = LocalLayoutDirection.current

    val context = remember(alignment, layoutDirection) {
        HollerAnimationContext(alignment, layoutDirection)
    }

    val descriptions = remember(
        closeContentDescription,
        expandContentDescription,
        collapseContentDescription,
    ) {
        ControlDescriptions(
            close = closeContentDescription,
            expand = expandContentDescription,
            collapse = collapseContentDescription,
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(contentPadding),
        contentAlignment = alignment,
    ) {
        HostedMessages(
            holler = holler,
            stackingStyle = stackingStyle,
            maxVisible = maxVisible,
            // The cards behind the front one show on the side away from the nearest screen edge.
            peeksDown = alignment is BiasAlignment && alignment.verticalBias < 0f,
        ) { entry, closeGap, pile ->
            HostedHoller(
                entry = entry,
                holler = holler,
                context = context,
                closeGap = closeGap,
                spacing = if (pile == null) {
                    spacing
                } else {
                    0.dp
                },
                colors = colors,
                style = style,
                descriptions = descriptions,
                animation = animation,
                animateOnPress = animateOnPress,
                pile = pile,
                expandIcon = expandIcon,
                closeIcon = closeIcon,
                message = message,
            )
        }
    }
}

/**
 * Arranges the messages a host shows at the same time, one under the other or as a pile, and picks
 * which ones those are.
 *
 * @param holler the object whose messages are drawn.
 * @param stackingStyle how the messages are arranged.
 * @param maxVisible how many messages may be on screen at once.
 * @param peeksDown whether, in a pile, the cards behind show below the front card and not above it.
 * @param message draws one message. It receives the message, whether the space it leaves should close
 * smoothly, and its place in the pile, which is `null` when the messages are one under the other.
 */
@Composable
private fun HostedMessages(
    holler: Holler,
    stackingStyle: HollerStackingStyle,
    maxVisible: Int,
    peeksDown: Boolean,
    message: @Composable (entry: HollerEntry, closeGap: Boolean, pile: PilePlace?) -> Unit,
) {
    when (stackingStyle) {
        HollerStackingStyle.Column -> {
            val visible = holler.entries.take(maxVisible)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                visible.forEach { entry ->
                    key(entry.id) { message(entry, visible.size > 1, null) }
                }
            }
        }

        HollerStackingStyle.Pile -> {
            // The newest messages are the ones on the pile, and the one before them waits behind it,
            // invisible, ready to fade in. Earlier ones are not drawn.
            val hidden = holler.entries.dropLast(maxVisible + 1)

            hidden.forEach { entry ->
                key(entry.id) { RemoveWhenClosed(entry, holler) }
            }

            Pile(
                entries = holler.entries.takeLast(maxVisible + 1),
                size = maxVisible,
                peeksDown = peeksDown,
            ) { entry, place ->
                message(entry, false, place)
            }
        }
    }
}

/**
 * Takes a message that is not drawn out of the queue once it is closed. A drawn message is removed
 * when its exit animation ends; one that is not drawn has no animation to wait for.
 *
 * @param entry the message that is not drawn.
 * @param holler the object the message belongs to.
 */
@Composable
private fun RemoveWhenClosed(entry: HollerEntry, holler: Holler) {
    // Read the animation target first: it is the observable part, and it turns off when the message closes.
    val closed = !entry.visibility.targetState && entry.isDismissed

    if (closed) {
        LaunchedEffect(entry) { holler.remove(entry) }
    }
}

/**
 * One message inside the host: starts its enter animation when first drawn and removes it from the
 * queue once its exit animation has finished. When the message is replaced it draws the new content
 * in a new card, without animating. The animation itself is played around the card, by
 * `HollerMessage(…)`, because that is where a message can ask for an animation of its own.
 *
 * @param entry the message to draw.
 * @param holler the object the message belongs to.
 * @param context where and how messages are shown, for the animation.
 * @param closeGap whether the space the message occupied should shrink smoothly as it leaves, so
 * the other messages glide into place. Only needed when several messages are stacked.
 * @param spacing space kept under the message, separating it from the next one.
 * @param colors the host's colors, used unless the message sets its own.
 * @param style the host's style, used unless the message sets its own.
 * @param descriptions what a screen reader announces for the message's own buttons.
 * @param animation the host's enter and exit animation, used unless the message sets its own.
 * @param animateOnPress whether the message gives under a press.
 * @param pile the place of the message in a pile, or `null` when the messages are one under the other.
 * @param expandIcon draws the symbol of the expand button. It receives whether the message is expanded.
 * @param closeIcon draws the symbol of the close button.
 * @param message draws a message that was shown as a value.
 */
@Composable
private fun HostedHoller(
    entry: HollerEntry,
    holler: Holler,
    context: HollerAnimationContext,
    closeGap: Boolean,
    spacing: Dp,
    colors: HollerColors,
    style: HollerStyle,
    descriptions: ControlDescriptions,
    animation: HollerAnimation,
    animateOnPress: Boolean,
    pile: PilePlace?,
    expandIcon: @Composable (expanded: Boolean) -> Unit,
    closeIcon: @Composable () -> Unit,
    message: @Composable HollerHostScope.(value: Any) -> Unit,
) {
    LaunchedEffect(entry) { entry.markShown() }

    val visibility = entry.visibility

    // Read the animation state first: it is the observable part, and reading it is what makes
    // this function run again when the exit animation ends.
    val exitFinished = visibility.isIdle && !visibility.currentState

    if (exitFinished && entry.wasShown && entry.isDismissed) {
        LaunchedEffect(entry) { holler.remove(entry) }
    }

    val current = entry.content

    val frame = HostedFrame(
        entry = entry,
        context = context,
        animation = animation,
        closeGap = closeGap,
        spacing = spacing,
        colors = colors,
        style = style,
        descriptions = descriptions,
        animateOnPress = animateOnPress,
        pile = pile,
        expandIcon = expandIcon,
        closeIcon = closeIcon,
        // A new mark for every content, so a replacement that the app does not draw is dropped too.
        drawn = remember(current) { DrawnMark() },
    )

    HostedValue(frame = frame, holler = holler, value = current.value, message = message)
}
