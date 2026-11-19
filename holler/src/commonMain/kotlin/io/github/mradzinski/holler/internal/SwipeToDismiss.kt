package io.github.mradzinski.holler.internal

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** How fast a card must be flung sideways to be dismissed whatever the distance, per second. */
private val SwipeFlingVelocity: Dp = 800.dp

/** How long a swiped card takes to finish leaving, in milliseconds. */
private const val SWIPE_SETTLE_MILLIS: Int = 150

/** The share of its own width a card must be dragged to be dismissed on release. */
private const val SWIPE_DISMISS_FRACTION: Float = 1f / 3f

/**
 * The two halves of swipe-to-dismiss. They are separate modifiers because they belong at different
 * places in the card's modifier chain: the movement and fading must wrap the card *and* its shadow,
 * while the gesture must only react to touches on the card itself.
 *
 * @param layer moves and fades the card. Apply it outside the shadow bleed padding, so the layer it
 * creates is large enough to contain the shadow.
 * @param gesture detects the horizontal drag. Apply it on the card itself.
 * @param isDragging reads whether the card is being dragged, or was swiped away.
 * @param wasSwipedAway reads whether the card was let go far or fast enough to be dismissed.
 */
internal class SwipeToDismiss(
    val layer: Modifier,
    val gesture: Modifier,
    private val isDragging: () -> Boolean,
    private val wasSwipedAway: () -> Boolean,
) {

    /** `true` from the moment a release is known to dismiss the card, while it finishes leaving. Observable. */
    val isLeaving: Boolean get() = wasSwipedAway()

    /**
     * `true` from the start of a drag until the card is let go without being dismissed. A release
     * that dismisses the card leaves it `true`, so whatever the drag was holding still, such as the
     * timer, stays held while the card leaves. Observable.
     */
    val isHeld: Boolean get() = isDragging()
}

/**
 * Creates the modifiers that let the card be swiped away horizontally. The card follows the
 * finger and fades as it moves; released past a third of its width or flung fast enough it
 * leaves and [onDismissed] is called, otherwise it springs back.
 *
 * @param onDismissed called once the card has been swiped away.
 * @return the movement and the gesture modifiers; see [SwipeToDismiss] for where each one goes.
 */
@Composable
internal fun rememberSwipeToDismiss(onDismissed: () -> Unit): SwipeToDismiss {
    val currentOnDismissed by rememberUpdatedState(onDismissed)
    var dragging by remember { mutableStateOf(false) }
    var swipedAway by remember { mutableStateOf(false) }
    val offset = remember { mutableFloatStateOf(0f) }
    var width by remember { mutableIntStateOf(0) }
    val flingVelocity = with(LocalDensity.current) { SwipeFlingVelocity.toPx() }
    val draggableState = rememberDraggableState { delta -> offset.floatValue += delta }

    val layer = Modifier.graphicsLayer {
        translationX = offset.floatValue
        alpha = if (width == 0) {
            1f
        } else {
            1f - (abs(offset.floatValue) / width).coerceIn(0f, 1f)
        }
    }

    val gesture = Modifier
        .onSizeChanged { width = it.width }
        .draggable(
            state = draggableState,
            orientation = Orientation.Horizontal,
            onDragStarted = { dragging = true },
            onDragStopped = { velocity ->
                val cardWidth = width.toFloat()
                val start = offset.floatValue
                val farEnough = abs(start) > cardWidth * SWIPE_DISMISS_FRACTION
                val fastEnough = abs(velocity) > flingVelocity && velocity * start > 0f

                if (cardWidth > 0f && (farEnough || fastEnough)) {
                    swipedAway = true

                    animate(
                        initialValue = start,
                        targetValue = if (start > 0f) {
                            cardWidth
                        } else {
                            -cardWidth
                        },
                        animationSpec = tween(SWIPE_SETTLE_MILLIS),
                    ) { value, _ ->
                        offset.floatValue = value
                    }

                    currentOnDismissed()
                } else {
                    dragging = false

                    animate(
                        initialValue = start,
                        targetValue = 0f,
                        animationSpec = spring(),
                    ) { value, _ ->
                        offset.floatValue = value
                    }
                }
            },
        )

    return remember(layer, gesture) {
        SwipeToDismiss(
            layer = layer,
            gesture = gesture,
            isDragging = { dragging },
            wasSwipedAway = { swipedAway },
        )
    }
}
