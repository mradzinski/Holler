package io.github.mradzinski.holler

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset

/**
 * Everything about a message's look that is not a color: shape, sizes, text styles and shadow
 * geometry.
 *
 * Create one with [HollerDefaults.style] and change only what you need.
 *
 * @param shape outline of the card.
 * @param maxWidth the widest the card may grow. It fills the available width up to this value.
 * @param iconSize size of the box the `icon` slot is placed in.
 * @param titleTextStyle text style of the title. Its color, if unspecified, comes from [HollerColors.title].
 * @param detailsTextStyle text style of the long text. Its color, if unspecified, comes from [HollerColors.details].
 * @param countdownTextStyle text style of the countdown. Its color, if unspecified, comes from [HollerColors.countdown].
 * @param shadowRadius how far the shadow blurs out. Zero draws it with a hard edge.
 * @param shadowSpread how much the shadow grows before blurring.
 * @param shadowOffset how far the shadow is shifted from the card.
 * @param progressBarHeight thickness of the progress bar.
 */
@Immutable
public data class HollerStyle(
    val shape: Shape,
    val maxWidth: Dp,
    val iconSize: Dp,
    val titleTextStyle: TextStyle,
    val detailsTextStyle: TextStyle,
    val countdownTextStyle: TextStyle,
    val shadowRadius: Dp,
    val shadowSpread: Dp,
    val shadowOffset: DpOffset,
    val progressBarHeight: Dp,
)
