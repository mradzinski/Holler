package io.github.mradzinski.holler.internal

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The height the cards of a pile share. The cards behind take the height of the front card, and when
 * another card becomes the front one that height moves to the new one over the time the pile takes
 * to move, so no card changes size in one step.
 *
 * It is used while the pile is measured, in two steps: [forcedFrontHeight] before the front card is
 * measured, and [sharedHeight] after.
 *
 * Two cases, because the height of the new front card is learned in different ways:
 * - A message arrives in front. It is measured freely, so its height is known at once, and the cards
 * behind move to it.
 * - The front card leaves and the one behind comes forward. That card is held at the moving height
 * until it gets there, so where it is going has to be known before it is measured freely: it is the
 * height recorded the last time it was in front. Without a record for the width it has now, it takes
 * its own height in one step, and the cards behind follow in one step too.
 *
 * @param scope where the movement runs: the scope of the pile.
 */
@Stable
internal class PileHeight(private val scope: CoroutineScope) {

    /** The front card the last time the pile was measured, by the id of its message. */
    private var frontId: Long? = null

    /** The messages that were in the pile the last time it was measured, by id. */
    private var known: Set<Long> = emptySet()

    /** The height the cards shared the last time the pile was measured, in pixels. `0` before any. */
    private var last: Int = 0

    /** The height the movement is heading for, in pixels. */
    private var target: Int = 0

    /** `true` when a message arrived in front and its height is not known yet. */
    private var awaitsArrival: Boolean = false

    /** `true` while the front card is held at the moving height, because it came forward from behind. */
    private var holdsFront: Boolean = false

    /** The running movement, if there is one. */
    private var movement: Job? = null

    /** The shared height while it moves, in pixels. Observable, so every step measures the pile again. */
    private var moving: Float by mutableFloatStateOf(0f)

    /** `true` while the shared height is moving. Observable, so its end measures the pile again. */
    private var isMoving: Boolean by mutableStateOf(false)

    /** The height the cards shared the last time the pile was measured, in pixels, or `null` before any. */
    val lastHeight: Int? get() = last.takeIf { it > 0 }

    /**
     * Notes which card is in front now and says whether it must be held at a height while it is measured.
     *
     * @param front the front card, or `null` when the pile is empty.
     * @param entries the messages of the pile.
     * @param width the most width a card is allowed, in pixels.
     * @return the height to measure the front card at, in pixels, or `null` to measure it freely.
     */
    fun forcedFrontHeight(
        front: HollerEntry?,
        entries: List<HollerEntry>,
        width: Int,
    ): Int? {
        val previous = frontId

        if (front == null) {
            stop()
            last = 0
        } else if (front.id != previous) {
            val cameForward = previous != null && front.id in known

            // Only a card that came forward is held, and only when the height it is going to is known.
            val recorded = front.content.cardState
                .frontHeight(width)
                .takeIf { cameForward }

            stop()
            awaitsArrival = previous != null && !cameForward

            if (recorded != null && last > 0 && recorded != last) {
                move(from = last.toFloat(), to = recorded, holdFront = true)
            }
        }

        frontId = front?.id
        known = entries.mapTo(mutableSetOf()) { it.id }

        return moving
            .roundToInt()
            .takeIf { isMoving && holdsFront }
    }

    /**
     * Gives the height the cards behind the front one take, once the front card has been measured.
     *
     * @param front the front card.
     * @param width the most width a card is allowed, in pixels.
     * @param freeHeight the height the front card took when it was measured freely, in pixels, or
     * `null` when it was held at the height [forcedFrontHeight] gave.
     * @return the shared height in pixels.
     */
    fun sharedHeight(
        front: HollerEntry,
        width: Int,
        freeHeight: Int?,
    ): Int {
        if (freeHeight != null) {
            front.content.cardState.recordFrontHeight(width, freeHeight)
            follow(freeHeight)
        }

        val shared = if (isMoving) {
            moving.roundToInt()
        } else {
            freeHeight ?: target
        }

        last = shared

        return shared
    }

    /**
     * Makes the shared height go to the height the front card has now: by moving when a message has
     * just arrived in front, and when a movement is already on its way to another height.
     *
     * @param height the height of the front card, in pixels.
     */
    private fun follow(height: Int) {
        val arrived = awaitsArrival

        awaitsArrival = false

        if (arrived && last > 0 && height != last) {
            move(from = last.toFloat(), to = height, holdFront = false)
        } else if (isMoving && height != target) {
            move(from = moving, to = height, holdFront = false)
        }
    }

    /**
     * Starts moving the shared height.
     *
     * @param from the height to start from, in pixels.
     * @param to the height to end at, in pixels.
     * @param holdFront whether the front card is held at the moving height too.
     */
    private fun move(
        from: Float,
        to: Int,
        holdFront: Boolean,
    ) {
        movement?.cancel()
        target = to
        moving = from
        holdsFront = holdFront
        isMoving = true

        movement = scope.launch {
            animate(
                initialValue = from,
                targetValue = to.toFloat(),
                animationSpec = tween(PILE_MOVE_MILLIS),
            ) { value, _ ->
                moving = value
            }

            isMoving = false
        }
    }

    /** Ends the movement where it is. */
    private fun stop() {
        movement?.cancel()
        movement = null
        isMoving = false
        holdsFront = false
        awaitsArrival = false
    }
}

/**
 * Creates the shared height of one pile.
 *
 * @return the shared height, kept for as long as the pile is drawn.
 */
@Composable
internal fun rememberPileHeight(): PileHeight {
    val scope = rememberCoroutineScope()

    return remember(scope) { PileHeight(scope) }
}
