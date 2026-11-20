package io.github.mradzinski.holler.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.mradzinski.holler.HollerDuration

/**
 * What a card has to keep for as long as its message shows the same content, even while the card
 * itself is not drawn: its timer, whether it is expanded, and how tall it was when it was last the
 * front card of a pile. A message that goes too far under a pile loses its card, and gets a new one
 * when it comes back; this is what makes the new card go on where the old one was.
 *
 * It belongs to one [HollerContent], so replaced content starts with a new one. It is only touched
 * while composing, measuring or handling input, which all happen on the UI thread.
 */
internal class CardState {

    /** The time on screen the [timer] was made for. */
    private var timerDuration: HollerDuration? = null

    /** The timer of the card, once a card has asked for one. */
    private var timer: HollerTimer? = null

    /** Whether the card is expanded, once a card has asked. */
    private var expansion: CardExpansion? = null

    /** The width a front card was given and the height it took, the last time it was measured freely. */
    private var frontSize: Pair<Int, Int>? = null

    /**
     * Gives the timer of the card: the one it already has when the time on screen is the same, so the
     * time that passed is kept, and a new one otherwise.
     *
     * @param duration the time on screen the card asks for.
     * @return the timer to count with.
     */
    fun timerFor(duration: HollerDuration): HollerTimer {
        val current = timer

        if (current != null && timerDuration == duration) return current

        timerDuration = duration

        return HollerTimer(duration.value).also { timer = it }
    }

    /**
     * Gives the expansion of the card: the one it already has, or a new one in its starting state.
     *
     * @param startsExpanded whether a new card starts with its long text revealed.
     * @return whether the card is expanded, and whether the user has expanded it yet.
     */
    fun expansionFor(startsExpanded: Boolean): CardExpansion {
        val current = expansion ?: CardExpansion(startsExpanded)

        expansion = current

        return current
    }

    /**
     * Records how tall the card is as the front card of a pile.
     *
     * @param width the most width the card was allowed, in pixels.
     * @param height the height it took, in pixels.
     */
    fun recordFrontHeight(width: Int, height: Int) {
        frontSize = width to height
    }

    /**
     * How tall the card was the last time it was the front card of a pile, if that is still true for
     * the width it is given now.
     *
     * @param width the most width the card is allowed now, in pixels.
     * @return the recorded height in pixels, or `null` when there is none for this width.
     */
    fun frontHeight(width: Int): Int? = frontSize
        ?.takeIf { it.first == width }
        ?.second
}

/**
 * Whether a card is expanded, and whether the user has expanded it yet.
 *
 * @param startExpanded whether the card starts with its long text revealed.
 */
internal class CardExpansion(startExpanded: Boolean) {

    /** `true` while the long text is revealed. */
    var isExpanded: Boolean by mutableStateOf(startExpanded)
        private set

    /** `true` once the user has expanded the card, after which the expand button no longer hints. */
    var hintSeen: Boolean by mutableStateOf(false)
        private set

    /**
     * `true` once the expand button has made its hinting movement, so a card that is drawn again does
     * not repeat it.
     */
    var hintNudged: Boolean = false

    /**
     * Expands a collapsed card, or collapses an expanded one.
     *
     * @return whether the card is now expanded.
     */
    fun toggle(): Boolean {
        isExpanded = !isExpanded

        if (isExpanded) hintSeen = true

        return isExpanded
    }
}
