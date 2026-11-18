package io.github.mradzinski.holler.internal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.mradzinski.holler.HollerActionsScope
import io.github.mradzinski.holler.HollerColors
import io.github.mradzinski.holler.HollerStyle
import kotlin.time.Duration

/** Minimum height of the footer strip that holds the countdown, the actions and the close button. */
private val FooterMinHeight: Dp = 30.dp

/**
 * Space after the close button when it sits in the footer strip. Chosen so the button is centered
 * under the place of the close button in the top row.
 */
private val FooterControlEndPadding: Dp = 14.dp

/** Size of the close button when it sits in the footer strip. */
private val FooterControlSize: Dp = 28.dp

/** Smallest space kept between the things in the footer strip. */
private val FooterGap: Dp = 8.dp

/**
 * The bottom of the card: a strip holding the countdown on the start side, then the actions and the
 * close button when they are placed at the bottom; and the progress bar under the strip.
 *
 * The countdown and the progress bar are only shown while the timer is still relevant: they leave for
 * good once the timer is stopped, and are never shown for an infinite duration. A strip that holds
 * actions or the close button stays for as long as the message does.
 *
 * @param timer the timer of the message.
 * @param countdown draws the time left, or `null` for no countdown.
 * @param actions the call-to-action content when it is placed at the bottom, or `null`.
 * @param scope the receiver given to the actions, which lets them close the message.
 * @param showClose whether the close button sits in the strip.
 * @param showProgressBar whether the progress bar is drawn.
 * @param colors the colors of the card.
 * @param style the style of the card.
 * @param closeContentDescription what a screen reader announces for the close button.
 * @param closeIcon draws the symbol of the close button.
 * @param onClose called when the close button is pressed.
 */
@Composable
internal fun CardFooter(
    timer: HollerTimer,
    countdown: (@Composable (remaining: Duration) -> Unit)?,
    actions: (@Composable HollerActionsScope.() -> Unit)?,
    scope: HollerActionsScope,
    showClose: Boolean,
    showProgressBar: Boolean,
    colors: HollerColors,
    style: HollerStyle,
    closeContentDescription: String,
    closeIcon: @Composable () -> Unit,
    onClose: () -> Unit,
) {
    val staysWithMessage = actions != null || showClose
    val collapse = shrinkVertically(tween(FOOTER_COLLAPSE_MILLIS)) + fadeOut(tween(FOOTER_COLLAPSE_MILLIS))

    AnimatedVisibility(
        visible = staysWithMessage || (countdown != null && timer.isActive),
        exit = collapse,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.footerContainer)
                .heightIn(min = FooterMinHeight)
                .padding(
                    start = EdgePadding,
                    end = if (showClose) {
                        FooterControlEndPadding
                    } else {
                        EdgePadding
                    },
                ),
        ) {
            // The countdown takes the start side and all the free width, which pushes the actions and
            // the close button to the end side even when there is no countdown.
            Box(Modifier.weight(1f)) {
                if (countdown != null) {
                    FooterCountdown(timer, countdown, colors, style)
                }
            }

            if (actions != null) {
                Spacer(Modifier.width(FooterGap))
                ActionsRow(scope, actions)
            }

            if (showClose) {
                Spacer(Modifier.width(FooterGap))
                ControlButton(
                    label = closeContentDescription,
                    size = FooterControlSize,
                    colors = colors,
                    onClick = onClose,
                    icon = closeIcon,
                )
            }
        }
    }

    AnimatedVisibility(visible = showProgressBar && timer.isActive, exit = collapse) {
        ProgressBar(timer, colors, style.progressBarHeight)
    }
}

/**
 * The countdown inside the footer strip, in its text style. It fades away when the timer is stopped,
 * which matters when the strip itself stays because it also holds actions or the close button.
 *
 * @param timer the timer of the message.
 * @param countdown draws the time left.
 * @param colors the colors of the card.
 * @param style the style of the card.
 */
@Composable
private fun FooterCountdown(
    timer: HollerTimer,
    countdown: @Composable (remaining: Duration) -> Unit,
    colors: HollerColors,
    style: HollerStyle,
) {
    AnimatedVisibility(visible = timer.isActive, exit = fadeOut(tween(FOOTER_COLLAPSE_MILLIS))) {
        ProvideHollerTextStyle(style.countdownTextStyle, colors.countdown) {
            CountdownSlot(timer, countdown)
        }
    }
}

/**
 * Calls the caller's countdown with the time left. The time is read here and nowhere else, so
 * this small function is the only thing that recomposes when the second changes.
 *
 * @param timer the timer of the message.
 * @param countdown draws the time left.
 */
@Composable
private fun CountdownSlot(timer: HollerTimer, countdown: @Composable (remaining: Duration) -> Unit) {
    val remaining by timer.remaining

    countdown(remaining)
}

/**
 * The progress bar. It reads the timer only while drawing, so it animates by redrawing and never
 * causes a recomposition. It fills from the start side, following the layout direction.
 *
 * @param timer the timer of the message.
 * @param colors the colors of the card.
 * @param height thickness of the bar.
 */
@Composable
private fun ProgressBar(
    timer: HollerTimer,
    colors: HollerColors,
    height: Dp,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .drawBehind {
                drawRect(colors.progressTrack)

                val filled = size.width * timer.progress

                val left = if (layoutDirection == LayoutDirection.Ltr) {
                    0f
                } else {
                    size.width - filled
                }

                drawRect(
                    colors.progress,
                    topLeft = Offset(left, 0f),
                    size = Size(filled, size.height),
                )
            },
    )
}
