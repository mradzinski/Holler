package io.github.mradzinski.holler

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Diameter of [HollerIconRing] by default: the size of the box a message gives its `icon` slot. */
private val DefaultIconSize: Dp = 22.dp

/** The ring of [HollerIconRing] is this many times thinner than the icon is wide. */
private const val RING_THICKNESS_DIVISOR: Float = 11f

/** Share of the diameter of [HollerIconRing] given to the icon inside the ring. */
private const val INNER_ICON_FRACTION: Float = 0.72f

/**
 * Puts an icon inside a thin round ring, for the `icon` slot of a message:
 * `icon = { HollerIconRing { Icon(Icons.Default.Check, contentDescription = null) } }`.
 *
 * The library has no icons of its own: the icon inside the ring is the app's, and so is its color.
 * The ring and the icon share [tint]; an icon that sets no tint of its own is drawn in it, because it
 * is the content color inside the ring. The icon is made to fit the ring, whatever size it asks for.
 *
 * The `icon` slot of a message takes any composable, so this is optional: an icon can also be placed
 * in the slot directly, without a ring.
 *
 * @param modifier modifier applied to the ring.
 * @param tint color of the ring, and content color of the icon inside it. Defaults to the current
 * content color, which in a message is the title color.
 * @param size diameter of the ring. Defaults to the size of the `icon` slot of a message.
 * @param icon the icon drawn inside the ring, for example a Material `Icon`.
 */
@Composable
public fun HollerIconRing(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = DefaultIconSize,
    icon: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .drawBehind {
                val thickness = this.size.minDimension / RING_THICKNESS_DIVISOR

                drawCircle(
                    color = tint,
                    radius = (this.size.minDimension - thickness) / 2f,
                    style = Stroke(thickness),
                )
            },
    ) {
        CompositionLocalProvider(LocalContentColor provides tint) {
            // A box of a fixed size caps what the icon may take, so a 24dp Material icon shrinks to fit.
            Box(Modifier.size(size * INNER_ICON_FRACTION), contentAlignment = Alignment.Center) { icon() }
        }
    }
}
