package io.github.mradzinski.holler

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mradzinski.holler.internal.ChevronDownVector
import io.github.mradzinski.holler.internal.CloseVector
import io.github.mradzinski.holler.internal.EXPAND_MILLIS

/** Default values and ready-made building blocks for messages. */
public object HollerDefaults {

    /** Opacity of the shadow under a card whose theme is light. */
    private const val LIGHT_SHADOW_ALPHA: Float = 0.12f

    /** Opacity of the shadow under a card whose theme is dark, where a shadow needs more strength to show. */
    private const val DARK_SHADOW_ALPHA: Float = 0.40f

    /** A surface darker than this (0 is black, 1 is white) means the theme is a dark one. */
    private const val DARK_THEME_LUMINANCE: Float = 0.5f

    /** How many messages a host shows at once when they are one under the other. */
    private const val COLUMN_MAX_VISIBLE: Int = 3

    /** How many messages a host shows at once when they are a pile. */
    private const val PILE_MAX_VISIBLE: Int = 5

    /** A half turn in degrees: the expand chevron points up when rotated by this much. */
    private const val HALF_TURN_DEGREES: Float = 180f

    /**
     * The default colors: the ones of the app's [MaterialTheme], so a message matches the app it is
     * shown in, in light and in dark, with no setup. This is what a message uses when no colors are
     * given.
     *
     * To change a few of them, copy the result: `HollerDefaults.colors().copy(progress = Color.Red)`.
     * To build a look of your own, create a [HollerColors] with your own colors.
     *
     * @return the colors taken from the current [MaterialTheme].
     */
    @Composable
    public fun colors(): HollerColors = colors(MaterialTheme.colorScheme)

    /**
     * The colors a message takes from a Material color scheme: which role of the scheme is used for
     * which part of the message.
     *
     * | Part of the message | Role of the scheme |
     * |---|---|
     * | Card | `surfaceContainerHigh` |
     * | Title, and the icon unless it sets its own tint | `onSurface` |
     * | Long text, countdown, expand and close buttons | `onSurfaceVariant` |
     * | Outline of the card | `outlineVariant` |
     * | Footer strip | `surfaceContainerHighest` |
     * | Progress bar, hint dot | `primary` |
     * | Shadow | `scrim`, faint on a light scheme and stronger on a dark one |
     *
     * @param colorScheme the scheme to take the colors from.
     * @return the colors of a message for that scheme.
     */
    public fun colors(colorScheme: ColorScheme): HollerColors {
        val isDark = colorScheme.surface.luminance() < DARK_THEME_LUMINANCE
        val shadowAlpha = if (isDark) {
            DARK_SHADOW_ALPHA
        } else {
            LIGHT_SHADOW_ALPHA
        }

        return HollerColors(
            container = colorScheme.surfaceContainerHigh,
            title = colorScheme.onSurface,
            details = colorScheme.onSurfaceVariant,
            controls = colorScheme.onSurfaceVariant,
            border = colorScheme.outlineVariant,
            shadow = colorScheme.scrim.copy(alpha = shadowAlpha),
            footerContainer = colorScheme.surfaceContainerHighest,
            countdown = colorScheme.onSurfaceVariant,
            progress = colorScheme.primary,
            progressTrack = Color.Transparent,
        )
    }

    /**
     * The default shape, sizes, text styles and shadow. Every value can be replaced individually.
     *
     * The typeface and the shapes come from the app's [MaterialTheme] (the font family of its
     * `titleSmall`, `bodyMedium` and `bodySmall` styles, and its large shape).
     * Text sizes, weights and line heights are the library's own, because the layout of a message is
     * built around them; nothing else of the theme's text styles, such as letter spacing, is taken.
     *
     * @param shape outline of the card. Defaults to the theme's large shape.
     * @param maxWidth the widest the card may grow.
     * @param iconSize size of the box the `icon` slot is placed in.
     * @param titleTextStyle text style of the title.
     * @param detailsTextStyle text style of the long text.
     * @param countdownTextStyle text style of the countdown.
     * @param shadowRadius how far the shadow blurs out. Zero draws it with a hard edge.
     * @param shadowSpread how much the shadow grows before blurring.
     * @param shadowOffset how far the shadow is shifted from the card.
     * @param progressBarHeight thickness of the progress bar.
     * @return the style.
     */
    @Composable
    public fun style(
        shape: Shape = MaterialTheme.shapes.large,
        maxWidth: Dp = 375.dp,
        iconSize: Dp = 22.dp,
        titleTextStyle: TextStyle = TextStyle(
            fontFamily = MaterialTheme.typography.titleSmall.fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.5.sp,
            lineHeight = 22.sp,
        ),
        detailsTextStyle: TextStyle = TextStyle(
            fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.5.sp,
        ),
        countdownTextStyle: TextStyle = TextStyle(
            fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        ),
        shadowRadius: Dp = 24.dp,
        shadowSpread: Dp = 0.dp,
        shadowOffset: DpOffset = DpOffset(0.dp, 12.dp),
        progressBarHeight: Dp = 4.dp,
    ): HollerStyle = HollerStyle(
        shape = shape,
        maxWidth = maxWidth,
        iconSize = iconSize,
        titleTextStyle = titleTextStyle,
        detailsTextStyle = detailsTextStyle,
        countdownTextStyle = countdownTextStyle,
        shadowRadius = shadowRadius,
        shadowSpread = shadowSpread,
        shadowOffset = shadowOffset,
        progressBarHeight = progressBarHeight,
    )

    /**
     * How many messages a host shows at once unless it is told otherwise: 3 one under the other, and 5
     * as a pile, where the ones behind take almost no room.
     *
     * @param stackingStyle how the host arranges its messages.
     * @return the default for the `maxVisible` parameter of [HollerHost].
     */
    public fun maxVisible(stackingStyle: HollerStackingStyle): Int = when (stackingStyle) {
        HollerStackingStyle.Column -> COLUMN_MAX_VISIBLE
        HollerStackingStyle.Pile -> PILE_MAX_VISIBLE
    }

    /**
     * The symbol the expand button shows unless the host is given another: a chevron that points down
     * while the message is collapsed and turns to point up while it is expanded. It is drawn in the
     * content color of where it is placed, which inside the expand button is [HollerColors.controls].
     *
     * It is the default of the `expandIcon` parameter of [HollerHost]. Call it from your own `expandIcon`
     * to keep the chevron and add to it.
     *
     * @param expanded whether the message is expanded.
     * @param modifier modifier applied to the symbol.
     */
    @Composable
    public fun ExpandIcon(expanded: Boolean, modifier: Modifier = Modifier) {
        val rotation by animateFloatAsState(
            targetValue = if (expanded) {
                HALF_TURN_DEGREES
            } else {
                0f
            },
            animationSpec = tween(EXPAND_MILLIS),
        )

        Icon(
            imageVector = ChevronDownVector,
            contentDescription = null,
            modifier = modifier.graphicsLayer { rotationZ = rotation },
        )
    }

    /**
     * The symbol the close button shows unless the host is given another: a cross. It is drawn in the
     * content color of where it is placed, which inside the close button is [HollerColors.controls].
     *
     * It is the default of the `closeIcon` parameter of [HollerHost].
     *
     * @param modifier modifier applied to the symbol.
     */
    @Composable
    public fun CloseIcon(modifier: Modifier = Modifier) {
        Icon(
            imageVector = CloseVector,
            contentDescription = null,
            modifier = modifier,
        )
    }
}
