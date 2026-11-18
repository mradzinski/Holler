package io.github.mradzinski.holler.internal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mradzinski.holler.HollerActionsScope
import io.github.mradzinski.holler.HollerColors
import io.github.mradzinski.holler.HollerStyle

/** Space kept free on the end side of the long text and the actions. */
private val BodyEndPadding: Dp = 26.dp

/** Space under the last element of the body (long text or actions). */
private val BodyBottomPadding: Dp = 18.dp

/** Space between the long text and the actions under it. */
private val MessageActionsGap: Dp = 12.dp

/** Extra space under the header when nothing follows it, so a collapsed card is 60dp tall. */
private val CollapsedHeaderGap: Dp = 9.dp

/** How long the long text takes to fade in, at the end of the expansion, in milliseconds. */
private const val MESSAGE_FADE_IN_MILLIS: Int = 150

/** How long the long text takes to fade out, at the start of the collapse, in milliseconds. */
private const val MESSAGE_FADE_OUT_MILLIS: Int = 100

/**
 * How far from the start edge the long text and the actions begin, so that they line up with the
 * title: past the icon when there is one, at the edge padding otherwise.
 *
 * @param hasIcon whether the card shows an icon before the title.
 * @param style the style of the card, for the size of the icon.
 * @return the start padding of the card's body.
 */
internal fun bodyStartPadding(hasIcon: Boolean, style: HollerStyle): Dp = if (hasIcon) {
    EdgePadding + style.iconSize + IconGap
} else {
    EdgePadding
}

/**
 * Everything between the header and the footer: the long text, revealed by expanding, and the
 * actions when they are placed in the middle, which sit either inside the expandable part or always visible
 * under it.
 *
 * @param details the long text, or `null` when the card cannot be expanded.
 * @param actions the call-to-action content when it is placed in the middle, or `null`.
 * @param scope the receiver given to the actions, which lets them close the message.
 * @param expanded whether the long text is currently revealed.
 * @param actionsInBody `true` when the actions live inside the expandable part, `false` when
 * they are always visible.
 * @param startPadding space before the message and the actions, so they line up with the title.
 * @param colors the colors of the card.
 * @param style the style of the card.
 */
@Composable
internal fun CardBody(
    details: (@Composable () -> Unit)?,
    actions: (@Composable HollerActionsScope.() -> Unit)?,
    scope: HollerActionsScope,
    expanded: Boolean,
    actionsInBody: Boolean,
    startPadding: Dp,
    colors: HollerColors,
    style: HollerStyle,
) {
    val actionsOutside = actions != null && !actionsInBody

    val collapsedGap by animateDpAsState(
        targetValue = if (expanded || actionsOutside) {
            0.dp
        } else {
            CollapsedHeaderGap
        },
        animationSpec = tween(EXPAND_MILLIS),
    )

    Spacer(Modifier.height(collapsedGap))

    if (details != null) {
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(EXPAND_MILLIS)) +
                fadeIn(tween(MESSAGE_FADE_IN_MILLIS, delayMillis = EXPAND_MILLIS - MESSAGE_FADE_IN_MILLIS)),
            exit = fadeOut(tween(MESSAGE_FADE_OUT_MILLIS)) + shrinkVertically(tween(EXPAND_MILLIS)),
        ) {
            Column(
                Modifier.padding(
                    start = startPadding,
                    end = BodyEndPadding,
                    bottom = if (actionsOutside) {
                        MessageActionsGap
                    } else {
                        BodyBottomPadding
                    },
                ),
            ) {
                ProvideHollerTextStyle(style.detailsTextStyle, colors.details, details)

                if (actions != null && actionsInBody) {
                    Spacer(Modifier.height(MessageActionsGap))
                    ActionsRow(scope, actions)
                }
            }
        }
    }

    if (actions != null && actionsOutside) {
        Box(Modifier.padding(start = startPadding, end = BodyEndPadding, bottom = BodyBottomPadding)) {
            ActionsRow(scope, actions)
        }
    }
}

/**
 * The row of actions. The library sets no text style here: the actions are the app's own buttons,
 * which bring their own, and anything else is drawn in the card's content color.
 *
 * @param scope the receiver given to the actions, which lets them close the message.
 * @param actions the call-to-action content.
 */
@Composable
internal fun ActionsRow(scope: HollerActionsScope, actions: @Composable HollerActionsScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        scope.actions()
    }
}
