package io.github.mradzinski.holler.internal

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.pow

/** How much smaller each card of a pile is drawn than the card in front of it, as a share of its size. */
private const val PILE_SCALE_STEP: Float = 0.05f

/** How far each card of a pile shows past the card in front of it. */
private val PilePeek: Dp = 10.dp

/** How long a card takes to move forward or back in a pile, in milliseconds. */
private const val PILE_MOVE_MILLIS: Int = 200

/** The share of the press of the card in front of it that a covered card follows. */
private const val PILE_PRESS_FOLLOW: Float = 0.5f

/** How fast the content of a card fades as it goes behind another: gone a little past half a step back. */
private const val PILE_CONTENT_FADE: Float = 1.6f

/**
 * The link through which the front card of a pile tells the cards behind it how far it is pressed in,
 * so they can follow.
 */
@Stable
internal class PilePress {

    /** Reads the size the front card is drawn at under a press, as a share of its full size. Observable. */
    var frontScale: () -> Float by mutableStateOf({ 1f })
}

/**
 * The place of one card in a pile.
 *
 * @param covered whether another card is in front of this one. A covered card is not visible as a
 * message: its timer does not run and it takes no input.
 * @param depth reads how many cards are in front of this one. It passes through the values in between
 * while the card moves forward or back. Read it while drawing.
 * @param press the link between the front card's press and the cards behind it.
 */
internal class PilePlace(
    val covered: Boolean,
    val depth: () -> Float,
    val press: PilePress,
)

/**
 * How visible the content of a card is at a place in a pile: fully in front, and gone soon after the
 * card starts going behind another, so that only its surface shows.
 *
 * @return the opacity of the content, from `0` to `1`.
 */
internal fun PilePlace.contentAlpha(): Float = (1f - depth() * PILE_CONTENT_FADE).coerceIn(0f, 1f)

/**
 * How visible a card is at a place in a pile that holds [size] cards: fully while it is one of them,
 * and less and less on its way to or from the place just behind the last one, where it cannot be seen.
 * A card therefore fades in as it joins the back of the pile and fades out as it is pushed off it.
 *
 * @param depth how many cards are in front of the card, with the values in between while it moves.
 * @param size how many cards the pile holds.
 * @return the opacity of the card, from `0` to `1`.
 */
internal fun pileCardAlpha(depth: Float, size: Int): Float = (size - depth).coerceIn(0f, 1f)

/**
 * Lays messages out as a pile: the newest in front at full size, and the earlier ones behind it, each
 * a little smaller and showing past the one in front of it. The cards behind take the height of the
 * front card, so a taller one never shows around it.
 *
 * A message that is leaving is drawn over the pile where it was and no longer counts as part of it, so
 * the card behind it comes forward while it leaves.
 *
 * @param entries the messages of the pile, oldest first, and before them the one that waits right
 * behind it, if there is one. That one is drawn where the next card of the pile would be, invisible, so
 * that it can fade in when a place opens, and a card pushed off the pile can fade out the same way.
 * @param size how many cards the pile holds.
 * @param peeksDown whether the cards behind show below the front card. They show above it otherwise.
 * @param card draws one message, given its place in the pile.
 */
@Composable
internal fun Pile(
    entries: List<HollerEntry>,
    size: Int,
    peeksDown: Boolean,
    card: @Composable (entry: HollerEntry, place: PilePlace) -> Unit,
) {
    val press = remember { PilePress() }
    val staying = entries.filterNot { it.isLeaving }
    val frontIndex = entries.indexOf(staying.lastOrNull())

    Layout(
        content = {
            entries.forEach { entry ->
                key(entry.id) {
                    val index = staying.indexOf(entry)

                    PileSlot(
                        // A leaving card stays where it was: it has no place among the staying ones.
                        depth = rememberLastDepth(staying.lastIndex - index, isStaying = index >= 0),
                        leaving = index < 0,
                        size = size,
                        peeksDown = peeksDown,
                        press = press,
                    ) { place ->
                        card(entry, place)
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val front = measurables.getOrNull(frontIndex)?.measure(loose)

        // A front card that is not drawn yet has no height to give to the others.
        val frontHeight = front?.height?.takeIf { it > 0 }

        val placeables = measurables.mapIndexed { index, measurable ->
            when {
                index == frontIndex && front != null -> front
                frontHeight == null || entries[index].isLeaving -> measurable.measure(loose)
                else -> measurable.measure(loose.copy(minHeight = frontHeight, maxHeight = frontHeight))
            }
        }

        val width = placeables.maxOfOrNull { it.width } ?: 0
        val height = frontHeight ?: placeables.maxOfOrNull { it.height } ?: 0

        layout(width, height) {
            placeables.forEach { placeable ->
                placeable.place(
                    x = (width - placeable.width) / 2,
                    y = if (peeksDown) {
                        0
                    } else {
                        height - placeable.height
                    },
                )
            }
        }
    }
}

/** `true` while the message is on its way out: its exit animation is running. Observable. */
private val HollerEntry.isLeaving: Boolean get() = visibility.currentState && !visibility.targetState

/**
 * Remembers how many cards were in front of a card the last time it was part of the pile, so a card
 * that is leaving keeps the place it had.
 *
 * @param depth how many cards are in front of the card now. Only meaningful when [isStaying].
 * @param isStaying whether the card is still part of the pile.
 * @return [depth] for a card of the pile, and the last depth it had for one that is leaving.
 */
@Composable
private fun rememberLastDepth(depth: Int, isStaying: Boolean): Int {
    val last = remember { intArrayOf(0) }

    if (isStaying) last[0] = depth

    return last[0]
}

/**
 * One card of a pile: moves it to its place, makes it smaller the further back it is, and makes it
 * follow the press of the front card. A card behind another takes no input and is hidden from
 * assistive technology.
 *
 * @param depth how many cards are in front of this one.
 * @param leaving whether the card is on its way out, in which case it is drawn over the others.
 * @param size how many cards the pile holds. A card further back than that is not visible.
 * @param peeksDown whether cards behind show below the front card.
 * @param press the link between the front card's press and the cards behind it.
 * @param card draws the card, given its place.
 */
@Composable
private fun PileSlot(
    depth: Int,
    leaving: Boolean,
    size: Int,
    peeksDown: Boolean,
    press: PilePress,
    card: @Composable (place: PilePlace) -> Unit,
) {
    val animatedDepth = animateFloatAsState(
        targetValue = depth.toFloat(),
        animationSpec = tween(PILE_MOVE_MILLIS),
    )
    val covered = depth > 0 && !leaving

    // A covered card is marked as hidden and its semantics are left in place. Clearing them would
    // take its nodes away and bring the same ones back when the card comes forward, which the
    // accessibility layer of Compose for the web does not survive.
    val coveredModifier = if (covered) {
        Modifier
            .ignoreInput()
            .semantics { hideFromAccessibility() }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .zIndex(
                if (leaving) {
                    1f
                } else {
                    -depth.toFloat()
                },
            )
            .graphicsLayer {
                val back = animatedDepth.value
                val followed = 1f - (1f - press.frontScale()) * PILE_PRESS_FOLLOW.pow(back)

                // The front card shows its own press. The cards behind it follow, each less than the last.
                val pressScale = if (back > 0f) {
                    followed
                } else {
                    1f
                }
                val scale = (1f - PILE_SCALE_STEP * back) * pressScale

                // Scaling from the edge the card shows past keeps what shows the same for any height.
                transformOrigin = TransformOrigin(
                    pivotFractionX = 0.5f,
                    pivotFractionY = if (peeksDown) {
                        1f
                    } else {
                        0f
                    },
                )
                scaleX = scale
                scaleY = scale

                // Fading each thing the card draws, and not the card as one picture, keeps its shadow:
                // a picture of the card would be cut to the card's size.
                compositingStrategy = CompositingStrategy.ModulateAlpha
                alpha = pileCardAlpha(depth = back, size = size)
                translationY = PilePeek.toPx() * back * if (peeksDown) {
                    1f
                } else {
                    -1f
                }
            }
            .then(coveredModifier),
    ) {
        card(PilePlace(covered = covered, depth = { animatedDepth.value }, press = press))
    }
}

/**
 * Keeps everything inside from receiving touches and clicks, by taking them first.
 *
 * @return this modifier followed by the one that takes the input.
 */
private fun Modifier.ignoreInput(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial)
                .changes
                .forEach { it.consume() }
        }
    }
}
