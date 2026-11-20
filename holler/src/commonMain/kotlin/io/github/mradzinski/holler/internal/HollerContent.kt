package io.github.mradzinski.holler.internal

import io.github.mradzinski.holler.HollerDismissReason
import kotlinx.coroutines.CancellableContinuation

/**
 * What one call to `show` passed, and who is waiting to learn how it ends. Every call makes a new one,
 * and two of them are never equal, which is how the host tells that a message was replaced even when
 * the new value equals the old one.
 *
 * @param value what was shown: the app's own object, or a [TextMessage].
 */
internal class HollerContent(val value: Any) {

    /**
     * The callers suspended in `awaitDismiss` for this content. Only touched while holding the lock of
     * the [io.github.mradzinski.holler.Holler] the message belongs to.
     */
    val waiters: MutableList<CancellableContinuation<HollerDismissReason>> = mutableListOf()

    /**
     * What the card of this content keeps while it is not drawn: its timer and whether it is expanded.
     * New content has a new one, which is how a replaced message starts over.
     */
    val cardState: CardState = CardState()

    /**
     * Hands over the callers that are waiting and forgets them. The caller holds the lock, and resumes
     * them once it has released it.
     *
     * @return the callers that were waiting.
     */
    fun takeWaiters(): List<CancellableContinuation<HollerDismissReason>> {
        val waiting = waiters.toList()

        waiters.clear()

        return waiting
    }
}
