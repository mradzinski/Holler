package io.github.mradzinski.holler.sample.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.github.mradzinski.holler.HollerColors
import io.github.mradzinski.holler.HollerDefaults
import io.github.mradzinski.holler.HollerStyle
import io.github.mradzinski.holler.sample.resources.Res
import io.github.mradzinski.holler.sample.resources.inter_400
import io.github.mradzinski.holler.sample.resources.inter_500
import io.github.mradzinski.holler.sample.resources.inter_600
import org.jetbrains.compose.resources.Font

/*
 Two ways to theme messages, as a guide:

    1. Do nothing. `HollerDefaults.colors()` and `HollerDefaults.style()` take everything from the app's
       MaterialTheme, so messages match the app in light and in dark.

    2. Give messages a look of their own: a `HollerColors` with one color per part of the message
       (see `brandColors`) and a `HollerStyle` with its own shapes, typeface and shadow (see `brandStyle`).

 Either way the result is passed once to `HollerHost(colors = …, style = …)`, or per message to `show`.
*/

/** Deep indigo card of the brand look. */
private val BrandIndigo: Color = Color(0xFF312E81)

/** Pale lavender secondary text of the brand look. */
private val BrandLavender: Color = Color(0xFFC7D2FE)

/** Amber accent of the brand look: the progress bar, and the label of a message's action buttons. */
private val BrandAmber: Color = Color(0xFFFBBF24)

/** Light periwinkle buttons of the brand look. */
private val BrandPeriwinkle: Color = Color(0xFFA5B4FC)

/** Bright violet outline and hard shadow of the brand look. */
private val BrandViolet: Color = Color(0xFF6366F1)

/** Slightly brighter indigo footer strip of the brand look. */
private val BrandFooter: Color = Color(0xFF3730A3)

/** Translucent white track behind the progress bar of the brand look. */
private val BrandTrack: Color = Color(0x33FFFFFF)

/** Corner radius of the card in the brand look. */
private val BrandCornerRadius: Dp = 6.dp

/** How far the hard shadow of the brand look is shifted down and to the end side. */
private val BrandShadowShift: Dp = 6.dp

/** Thickness of the progress bar of the brand look. */
private val BrandProgressBarHeight: Dp = 6.dp

/**
 * The colors of the brand look, the same in light and in dark: an indigo card with white and lavender
 * text, and an amber progress bar on a visible track.
 *
 * @return the colors.
 */
internal fun brandColors(): HollerColors = HollerColors(
    container = BrandIndigo,
    title = Color.White,
    details = BrandLavender,
    controls = BrandPeriwinkle,
    border = BrandViolet,
    shadow = BrandViolet,
    footerContainer = BrandFooter,
    countdown = BrandLavender,
    progress = BrandAmber,
    progressTrack = BrandTrack,
)

/**
 * The typeface of the brand look: Inter, bundled with the sample in three weights. The rest of the app
 * keeps the theme's default typeface, so this also shows how messages can be set in a typeface of
 * their own.
 *
 * @return the font family.
 */
@Composable
internal fun brandFontFamily(): FontFamily = FontFamily(
    Font(Res.font.inter_400, FontWeight.Normal),
    Font(Res.font.inter_500, FontWeight.Medium),
    Font(Res.font.inter_600, FontWeight.SemiBold),
)

/**
 * The style of the brand look: squarer corners on the card, its own typeface
 * with a heavier title, a thicker progress bar and a hard shadow shifted down and to the end side,
 * with no blur.
 *
 * @return the style.
 */
@Composable
internal fun brandStyle(): HollerStyle {
    val defaults = HollerDefaults.style()
    val fontFamily = brandFontFamily()

    return defaults.copy(
        shape = RoundedCornerShape(BrandCornerRadius),
        titleTextStyle = defaults.titleTextStyle.copy(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold),
        detailsTextStyle = defaults.detailsTextStyle.copy(fontFamily = fontFamily),
        countdownTextStyle = defaults.countdownTextStyle.copy(fontFamily = fontFamily),
        shadowRadius = 0.dp,
        shadowOffset = DpOffset(BrandShadowShift, BrandShadowShift),
        progressBarHeight = BrandProgressBarHeight,
    )
}
