package io.github.mradzinski.holler

import androidx.compose.animation.core.MutableTransitionState
import io.kotest.matchers.booleans.shouldBeTrue

/**
 * Checks that a message has fully entered: it is visible and its enter animation has ended. The
 * receiver is the `visibility` of the message's entry in the queue.
 */
internal fun MutableTransitionState<Boolean>.shouldHaveEntered() {
    isIdle.shouldBeTrue()
    currentState.shouldBeTrue()
}
