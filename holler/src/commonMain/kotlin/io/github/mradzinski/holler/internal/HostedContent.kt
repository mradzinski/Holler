package io.github.mradzinski.holler.internal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import io.github.mradzinski.holler.Holler
import io.github.mradzinski.holler.HollerAnimation
import io.github.mradzinski.holler.HollerAnimationContext
import io.github.mradzinski.holler.HollerColors
import io.github.mradzinski.holler.HollerHostScope
import io.github.mradzinski.holler.HollerMessage
import io.github.mradzinski.holler.HollerStyle
import io.github.mradzinski.holler.HostedScope

/**
 * Everything the host has prepared for drawing one message, whoever draws it: the library, for a text
 * message, or the app, for a message shown as a value.
 *
 * @param entry the message.
 * @param context where and how the host shows its messages, for the animation.
 * @param animation the host's animation, used unless the message sets its own.
 * @param closeGap whether the space the message occupied should shrink smoothly as it leaves, so the
 * other messages glide into place. Only needed when several messages are stacked.
 * @param spacing space kept under the message, separating it from the next one.
 * @param colors the host's colors, used unless the message sets its own.
 * @param style the host's style, used unless the message sets its own.
 * @param descriptions what a screen reader announces for the message's own buttons.
 * @param animateOnPress whether the message gives under a press.
 * @param pile the place of the message in a pile, or `null` when the messages are one under the other.
 * @param expandIcon draws the symbol of the expand button. It receives whether the message is expanded.
 * @param closeIcon draws the symbol of the close button.
 * @param drawn where `HollerMessage(…)` records that it drew the message.
 */
internal class HostedFrame(
    val entry: HollerEntry,
    val context: HollerAnimationContext,
    val animation: HollerAnimation,
    val closeGap: Boolean,
    val spacing: Dp,
    val colors: HollerColors,
    val style: HollerStyle,
    val descriptions: ControlDescriptions,
    val animateOnPress: Boolean,
    val pile: PilePlace?,
    val expandIcon: @Composable (expanded: Boolean) -> Unit,
    val closeIcon: @Composable () -> Unit,
    val drawn: DrawnMark,
)

/** How long the space of a leaving message takes to close, in milliseconds. */
private const val GAP_CLOSE_MILLIS: Int = 200

/** How long the space of a leaving message waits before it starts closing, in milliseconds. */
private const val GAP_CLOSE_DELAY_MILLIS: Int = 150

/**
 * Plays the enter and exit animation of a message around its card. It is called by `HollerMessage(…)`
 * and not by the host, because the message says which animation it wants, and the animation has to be
 * known before the card first appears.
 *
 * @param animation the animation to play: the message's own, or the host's.
 * @param style the style the card is drawn with, which says how much room its shadow needs.
 * @param card draws the card. It receives the modifier for the card itself, which holds the room for
 * its shadow and the space under it, and the modifier for what is inside the card, which carries the
 * part of the animation that moves the content separately.
 */
@Composable
internal fun HostedFrame.AnimatedCard(
    animation: HollerAnimation,
    style: HollerStyle,
    card: @Composable (modifier: Modifier, contentModifier: Modifier) -> Unit,
) {
    val gapExit = if (closeGap) {
        shrinkVertically(
            animationSpec = tween(GAP_CLOSE_MILLIS, delayMillis = GAP_CLOSE_DELAY_MILLIS),
            clip = false,
        )
    } else {
        ExitTransition.None
    }

    // The enter and exit animations fade the card through a layer cut to this node's size. The bleed
    // and the matching padding below make that layer large enough to hold the card's shadow.
    val bleed = style.shadowExtent

    // The enter animation lives in the card that plays it. A card that takes the message over after it
    // started cannot go on with it: it is a new card, for a message replaced by a value the app draws
    // from another place in its code, or for one that comes back from under a pile. Playing the enter
    // again from the start would show the message arriving twice, so that card shows at once instead.
    val takesOver = remember { entry.enterStarted }

    SideEffect { entry.enterStarted = true }

    AnimatedVisibility(
        visibleState = entry.visibility,
        modifier = Modifier.shadowBleed(bleed),
        enter = if (takesOver) {
            EnterTransition.None
        } else {
            animation.messageEnter(context)
        },
        exit = animation.messageExit(context) + gapExit,
    ) {
        // Replaced content is drawn as a new card in the same place: its timer, expansion and swipe
        // start over. The key is here, inside the animation, so that a replace does not start the
        // enter animation again.
        key(entry.content) {
            card(
                Modifier
                    .padding(bleed)
                    .padding(bottom = spacing),
                Modifier
                    .animateEnterExit(
                        enter = if (takesOver) {
                            EnterTransition.None
                        } else {
                            animation.messageContentEnter(context)
                        },
                        exit = animation.messageContentExit(context),
                    )
                    // A card behind another one in a pile shows its surface only.
                    .graphicsLayer { alpha = pile?.contentAlpha() ?: 1f },
            )
        }
    }
}

/** Records whether the host's `message` drew a message, by calling `HollerMessage(…)` for it. */
internal class DrawnMark {

    /** `true` once the message was drawn. */
    var wasDrawn: Boolean = false
}

/**
 * The frame of the message being drawn, set by the host around its `message` lambda. It is how
 * `HollerMessage(…)` reaches what the host prepared. `null` outside a host.
 */
private val LocalHostedFrame: ProvidableCompositionLocal<HostedFrame?> = compositionLocalOf { null }

/**
 * Draws one message of the queue. A text message is drawn by the library itself, with the host's
 * look; any other value is handed to the app's `message` lambda. If the app draws nothing for it, the
 * message is dropped, so it does not hold a place among the visible messages for ever.
 *
 * @param frame what the host prepared for the message.
 * @param holler the object the message belongs to.
 * @param value what was shown: a [TextMessage] or the app's own object.
 * @param message the host's `message`, which gives the app's values a look.
 */
@Composable
internal fun HostedValue(
    frame: HostedFrame,
    holler: Holler,
    value: Any,
    message: @Composable HollerHostScope.(value: Any) -> Unit,
) {
    CompositionLocalProvider(LocalHostedFrame provides frame) {
        if (value is TextMessage) {
            HostedScope.HollerMessage(
                title = value.title,
                details = value.details,
                duration = value.duration,
            )
        } else {
            HostedScope.message(value)
        }
    }

    LaunchedEffect(frame.drawn) {
        if (!frame.drawn.wasDrawn) holler.remove(frame.entry)
    }
}

/**
 * Gives `HollerMessage(…)` the frame of the message it is drawing, and records that the message was drawn.
 *
 * @return what the host prepared for that message.
 */
@Composable
internal fun hostedFrame(): HostedFrame {
    val frame = checkNotNull(LocalHostedFrame.current) {
        "HollerMessage(…) must be called from the message lambda of a HollerHost."
    }

    frame.drawn.wasDrawn = true

    return frame
}
