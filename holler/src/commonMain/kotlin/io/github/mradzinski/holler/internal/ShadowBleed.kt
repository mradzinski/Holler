package io.github.mradzinski.holler.internal

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import io.github.mradzinski.holler.HollerStyle
import kotlin.math.absoluteValue

/**
 * How far the card's shadow can reach beyond the card on any side: its blur, its spread and its
 * shift added together. Anything that fades the card needs this much room around it.
 */
internal val HollerStyle.shadowExtent: Dp
    get() {
        val shift = maxOf(shadowOffset.x.value.absoluteValue, shadowOffset.y.value.absoluteValue).dp

        return shadowRadius + shadowSpread + shift
    }

/**
 * Lets the content be [extent] larger on every side than the space it reports to its parent, and
 * centers it on that space. Pair it with an equal `padding` further down the chain.
 *
 * Why it exists: fading something (a swipe, or an enter or exit animation) draws it into a layer
 * that is cut to the layer's own size. A shadow lies outside the card, so a layer the size of the
 * card cuts the shadow off and leaves only the slivers behind the rounded corners. Putting the
 * fading layer between this modifier and the padding makes the layer big enough to hold the
 * shadow, without the card taking any more room in the layout.
 *
 * @param extent how much larger than its reported size the content may be, on each side.
 * @return this modifier followed by the enlarging layout.
 */
internal fun Modifier.shadowBleed(extent: Dp): Modifier = layout { measurable, constraints ->
    val bleed = extent.roundToPx()
    val placeable = measurable.measure(
        constraints.offset(
            horizontal = bleed * 2,
            vertical = bleed * 2,
        ),
    )
    val width = (placeable.width - bleed * 2).coerceAtLeast(0)
    val height = (placeable.height - bleed * 2).coerceAtLeast(0)

    layout(width, height) { placeable.place(-bleed, -bleed) }
}
