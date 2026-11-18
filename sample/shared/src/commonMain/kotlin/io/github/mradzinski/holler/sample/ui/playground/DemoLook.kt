package io.github.mradzinski.holler.sample.ui.playground

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mradzinski.holler.HollerColors
import io.github.mradzinski.holler.HollerDefaults
import io.github.mradzinski.holler.HollerStyle

/*
 The look of the playground's "Custom colors and style" message. It belongs to that one message: it is
 passed to `HollerMessage(colors = …, style = …)`, so the message keeps it whatever look the host gives
 to the others. The look of a whole host is set the same way, with the same two types, on `HollerHost`;
 see `brandColors` and `brandStyle` in the theme package.
*/

/** Near-black slate card of the demo look. */
private val DemoSlate: Color = Color(0xFF0F172A)

/** Slightly lighter slate of the demo look: the footer strip, and the track of the progress bar under it. */
private val DemoFooter: Color = Color(0xFF1E293B)

/** Light gray long text of the demo look. */
private val DemoMist: Color = Color(0xFFCBD5E1)

/** Mid gray buttons and countdown of the demo look. */
private val DemoSteel: Color = Color(0xFF94A3B8)

/** Mint accent of the demo look: the progress bar, the icon and the label of the action button. */
private val DemoMint: Color = Color(0xFF34D399)

/** The mint glow around the card of the demo look, drawn as its shadow. */
private val DemoGlow: Color = Color(0x6634D399)

/** Corner radius of the card in the demo look. */
private val DemoCornerRadius: Dp = 28.dp

/** How far the glow of the demo look spreads around the card. */
private val DemoGlowRadius: Dp = 28.dp

/** Thickness of the progress bar of the demo look. */
private val DemoProgressBarHeight: Dp = 3.dp

/** Size of the title text of the demo look. */
private val DemoTitleSize: TextUnit = 17.sp

/**
 * The colors of the demo look, the same in light and in dark: a slate card with white and gray text,
 * no outline, a mint glow in place of a shadow, and a mint progress bar whose track blends into the
 * footer strip.
 *
 * @return the colors.
 */
internal fun demoColors(): HollerColors = HollerColors(
    container = DemoSlate,
    title = Color.White,
    details = DemoMist,
    controls = DemoSteel,
    border = Color.Transparent,
    shadow = DemoGlow,
    footerContainer = DemoFooter,
    countdown = DemoSteel,
    progress = DemoMint,
    progressTrack = DemoFooter,
)

/**
 * The style of the demo look, made by changing a few values of the default one: rounder corners, a
 * larger and heavier title, a thinner progress bar and a shadow that is not shifted, so it shows as
 * an even glow all around the card.
 *
 * @return the style.
 */
@Composable
internal fun demoStyle(): HollerStyle {
    val defaults = HollerDefaults.style()

    return defaults.copy(
        shape = RoundedCornerShape(DemoCornerRadius),
        titleTextStyle = defaults.titleTextStyle.copy(fontWeight = FontWeight.Bold, fontSize = DemoTitleSize),
        shadowRadius = DemoGlowRadius,
        shadowOffset = DpOffset.Zero,
        progressBarHeight = DemoProgressBarHeight,
    )
}
