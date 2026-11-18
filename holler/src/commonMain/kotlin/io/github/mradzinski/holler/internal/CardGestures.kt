package io.github.mradzinski.holler.internal

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember

/** The size of a card while it is pressed, as a share of its full size. */
private const val PRESSED_SCALE: Float = 0.97f

/** How long a card takes to shrink when pressed, and to grow back when it is leaving, in milliseconds. */
private const val PRESS_MILLIS: Int = 120

/**
 * How quickly the bounce of a released card dies out: 1 would be no bounce at all, 0 a bounce that
 * never ends. This value gives two or three visible swings.
 */
private const val RELEASE_DAMPING_RATIO: Float = 0.35f

/** The stiffness of the spring that returns a released card to its full size: one swing takes 200 ms. */
private const val RELEASE_STIFFNESS: Float = 987f

/**
 * The push a released card starts its way back with, in shares of its size per second. A spring let go
 * from rest only swings as far as it was pulled, and 3% is too short a pull for its bounce to be seen;
 * with this push the card grows to about 1.5% over its full size before it settles.
 */
private const val RELEASE_VELOCITY: Float = 0.93f

/**
 * What the host tells a card about where it stands among the messages.
 *
 * @param leaving whether the message was closed and the card is on its way out. A leaving card does
 * not bounce when it is let go.
 * @param animateOnPress whether the card gives under a press: drawn smaller while pressed, and
 * bouncing back when let go.
 * @param covered whether the card is behind another one in a pile. A covered card is not visible as a
 * message, so its timer does not run and it does not answer a press.
 * @param pilePress where the front card of a pile reports how far it is pressed in, for the cards
 * behind it to follow, or `null` when the messages are not a pile.
 */
internal class CardPlace(
    val leaving: Boolean,
    val animateOnPress: Boolean,
    val covered: Boolean = false,
    val pilePress: PilePress? = null,
)

/**
 * Everything the user can do to a card with a finger or a pointer other than using its buttons: swipe
 * it away, hold it, and feel it give under the press.
 *
 * @param swipe the sideways drag that closes the card.
 * @param pressOrHover whether the card is pressed or hovered.
 * @param scale reads the size the card is drawn at, as a share of its full size. Read it while
 * drawing, so the press animation redraws the card without recomposing it.
 * @param covered whether the card is behind another one in a pile.
 */
internal class CardGestures(
    val swipe: SwipeToDismiss,
    val pressOrHover: PressOrHover,
    val scale: () -> Float,
    private val covered: Boolean,
) {

    /**
     * `true` while the card's timer must not run: a drag, a press or a hovering pointer holds the card,
     * or the card is behind another one in a pile and so is not visible. Observable.
     */
    val holdsTimer: Boolean get() = covered || swipe.isHeld || pressOrHover.isActive
}

/**
 * Creates the gestures of one card.
 *
 * @param pauseOnPressOrHover whether pressing or hovering over the card holds its timer.
 * @param place where the card stands among the messages.
 * @param onSwipedAway called once the card has been swiped away.
 * @return the gestures, whose modifiers the card applies.
 */
@Composable
internal fun rememberCardGestures(
    pauseOnPressOrHover: Boolean,
    place: CardPlace,
    onSwipedAway: () -> Unit,
): CardGestures {
    val swipe = rememberSwipeToDismiss(onDismissed = onSwipedAway)
    val pressOrHover = rememberPressOrHover(enabled = pauseOnPressOrHover)

    // A swipe is known to dismiss the card when it is let go, a moment before the message is closed.
    val scale = rememberPressScale(
        pressed = place.animateOnPress && !place.covered && pressOrHover.isPressedOnCard,
        leaving = place.leaving || swipe.isLeaving,
    )

    // The front card of a pile is the one the cards behind it follow.
    if (!place.covered && !place.leaving) {
        SideEffect { place.pilePress?.frontScale = scale }
    }

    return remember(swipe, pressOrHover, scale, place.covered) {
        CardGestures(swipe, pressOrHover, scale, place.covered)
    }
}

/**
 * Animates the size of a card under a press. The card shrinks a little while it is pressed, which
 * covers a hold and the whole of a drag. Let go, it returns to its full size with a small bounce. A
 * card that is leaving, however its message was closed, returns to its full size without the bounce,
 * even while it is still pressed.
 *
 * @param pressed whether a finger or a mouse button is held down on the card itself.
 * @param leaving whether the card is on its way out.
 * @return reads the size to draw the card at, as a share of its full size.
 */
@Composable
private fun rememberPressScale(pressed: Boolean, leaving: Boolean): () -> Float {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(pressed, leaving) {
        when {
            leaving -> scale.animateTo(targetValue = 1f, animationSpec = tween(PRESS_MILLIS))

            pressed -> scale.animateTo(targetValue = PRESSED_SCALE, animationSpec = tween(PRESS_MILLIS))

            // Only a card that was pressed has anything to bounce back from.
            scale.value != 1f -> scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = RELEASE_DAMPING_RATIO, stiffness = RELEASE_STIFFNESS),
                initialVelocity = RELEASE_VELOCITY,
            )
        }
    }

    return remember(scale) { { scale.value } }
}
