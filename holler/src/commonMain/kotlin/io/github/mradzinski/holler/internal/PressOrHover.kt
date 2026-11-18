package io.github.mradzinski.holler.internal

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastAny

/**
 * Tells whether the card is being pressed or hovered. The two are detected separately, because a
 * finger held on the card is not reported as hovering: only a mouse pointer hovers.
 *
 * @param modifier watches the card for presses and for a hovering pointer. Apply it on the card itself.
 * @param isPressed reads whether a finger or a mouse button is held down on the card, when the card
 * is watched for that.
 * @param isHovered reads whether a mouse pointer is over the card, when the card is watched for that.
 * @param isTouched reads whether a finger or a mouse button is held down on the card itself and not on
 * one of its buttons, whether or not the card is watched for pressing and hovering.
 */
internal class PressOrHover(
    val modifier: Modifier,
    private val isPressed: () -> Boolean,
    private val isHovered: () -> Boolean,
    private val isTouched: () -> Boolean,
) {

    /** `true` while the card is pressed or hovered, which is what holds its timer. Observable. */
    val isActive: Boolean get() = isPressed() || isHovered()

    /**
     * `true` while a finger or a mouse button is held down on the card itself: a press that started on
     * one of the card's buttons or actions does not count, because the button answers it. It does not
     * depend on whether pressing pauses the timer. Observable.
     */
    val isPressedOnCard: Boolean get() = isTouched()
}

/**
 * Creates the watcher of presses and hovering for one card. A press lasts from the first finger or
 * mouse button going down on the card until the last one is lifted, wherever it has moved to in
 * between and whatever inside the card it landed on. Nothing is consumed, so the card's buttons and
 * its swipe work as before.
 *
 * @param enabled whether pressing and hovering are reported as holding the card, see
 * [PressOrHover.isActive]. A press on the card itself is reported by [PressOrHover.isPressedOnCard]
 * whatever this value is.
 * @return the modifier to apply on the card and the state it feeds.
 */
@Composable
internal fun rememberPressOrHover(enabled: Boolean): PressOrHover {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var pressed by remember { mutableStateOf(false) }
    var touched by remember { mutableStateOf(false) }

    val modifier = Modifier
        .hoverable(interactionSource)
        .pointerInput(Unit) {
            awaitEachGesture {
                // A button inside the card has seen this press first. If it took it, it has consumed it.
                val down = awaitFirstDown(requireUnconsumed = false)

                pressed = true
                touched = !down.isConsumed

                try {
                    while (awaitPointerEvent().changes.fastAny { it.pressed }) Unit
                } finally {
                    pressed = false
                    touched = false
                }
            }
        }

    return remember(modifier, enabled) {
        PressOrHover(
            modifier,
            isPressed = { enabled && pressed },
            isHovered = { enabled && hovered },
            isTouched = { touched },
        )
    }
}
