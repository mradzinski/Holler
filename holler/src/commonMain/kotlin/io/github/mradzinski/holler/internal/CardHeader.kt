package io.github.mradzinski.holler.internal

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mradzinski.holler.HollerActionsScope
import io.github.mradzinski.holler.HollerColors
import io.github.mradzinski.holler.HollerStyle
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/** Space above the header row. */
private val HeaderTopPadding: Dp = 14.dp

/** Small space always kept under the header row. */
private val HeaderBottomPadding: Dp = 5.dp

/** Space between the title and the buttons after it. */
private val TitleControlsGap: Dp = 8.dp

/** Size of the expand and close buttons in the header, and height of the header row. */
private val ControlSize: Dp = 32.dp

/** How far the expand button dips when it hints that actions are inside. */
private val HintNudgeDistance: Dp = 3.dp

/** How long the expand button waits before hinting, in milliseconds. */
private const val HINT_DELAY_MILLIS: Long = 500L

/** Length of each half of one hint movement, in milliseconds. */
private const val HINT_NUDGE_MILLIS: Int = 180

/** How many times the expand button moves when hinting. */
private const val HINT_NUDGE_COUNT: Int = 2

/**
 * The top row of the card: icon, title, the actions when they are placed at the top, then the expand
 * button and the close button.
 *
 * @param title the short message.
 * @param icon the icon shown before the title, or `null` for none.
 * @param actions the call-to-action content when it is placed at the top, or `null`.
 * @param scope the receiver given to the actions, which lets them close the message.
 * @param expandable whether the card has a long text to reveal.
 * @param expanded whether the long text is currently revealed.
 * @param showHint whether the expand button should hint that actions are inside.
 * @param nudgeHint whether the expand button should also make its hinting movement, which it makes once.
 * @param showClose whether the close button sits in this row.
 * @param colors the colors of the card.
 * @param style the style of the card.
 * @param descriptions what a screen reader announces for the buttons.
 * @param expandIcon draws the symbol of the expand button. It receives whether the message is expanded.
 * @param closeIcon draws the symbol of the close button.
 * @param onToggleExpanded called when the expand button is pressed.
 * @param onHintNudged called once the expand button has made its hinting movement.
 * @param onClose called when the close button is pressed.
 */
@Composable
internal fun CardHeader(
    title: @Composable () -> Unit,
    icon: (@Composable () -> Unit)?,
    actions: (@Composable HollerActionsScope.() -> Unit)?,
    scope: HollerActionsScope,
    expandable: Boolean,
    expanded: Boolean,
    showHint: Boolean,
    nudgeHint: Boolean,
    showClose: Boolean,
    colors: HollerColors,
    style: HollerStyle,
    descriptions: ControlDescriptions,
    expandIcon: @Composable (expanded: Boolean) -> Unit,
    closeIcon: @Composable () -> Unit,
    onToggleExpanded: () -> Unit,
    onHintNudged: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = EdgePadding,
                end = EdgePadding,
                top = HeaderTopPadding,
                bottom = HeaderBottomPadding,
            ),
    ) {
        if (icon != null) {
            Box(Modifier.height(ControlSize), contentAlignment = Alignment.Center) {
                Box(Modifier.size(style.iconSize), contentAlignment = Alignment.Center) { icon() }
            }

            Spacer(Modifier.width(IconGap))
        }

        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = ControlSize),
        ) {
            ProvideHollerTextStyle(style.titleTextStyle, colors.title, title)
        }

        Spacer(Modifier.width(TitleControlsGap))

        if (actions != null) {
            HeaderActions(scope, actions)
        }

        if (expandable) {
            ExpandButton(
                expanded = expanded,
                showHint = showHint,
                nudgeHint = nudgeHint,
                colors = colors,
                label = if (expanded) {
                    descriptions.collapse
                } else {
                    descriptions.expand
                },
                icon = expandIcon,
                onHintNudged = onHintNudged,
                onClick = onToggleExpanded,
            )
        }

        if (showClose) {
            ControlButton(
                label = descriptions.close,
                size = ControlSize,
                colors = colors,
                onClick = onClose,
                icon = closeIcon,
            )
        }
    }
}

/**
 * One of the card's own buttons: a Material icon button of a fixed size, around a symbol the host
 * supplies. The description is set on the button, so it is announced whatever the symbol is.
 *
 * @param label what a screen reader announces for the button.
 * @param size width and height of the button.
 * @param colors the colors of the card.
 * @param onClick called when the button is pressed.
 * @param modifier applied to the button, after its size.
 * @param icon draws the symbol of the button.
 */
@Composable
internal fun ControlButton(
    label: String,
    size: Dp,
    colors: HollerColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(contentColor = colors.controls),
        modifier = Modifier
            .size(size)
            .then(modifier)
            .semantics { contentDescription = label },
        content = icon,
    )
}

/**
 * The actions when they are placed in the top row. They are centered on the line of the card's own
 * buttons and take that line's height in the layout, so a button taller than the line, as a Material
 * button is, does not make the row taller: it reaches into the free space above and below the row.
 *
 * @param scope the receiver given to the actions, which lets them close the message.
 * @param actions the call-to-action content.
 */
@Composable
private fun HeaderActions(scope: HollerActionsScope, actions: @Composable HollerActionsScope.() -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(ControlSize)
            .wrapContentHeight(unbounded = true),
    ) {
        ActionsRow(scope, actions)
    }

    Spacer(Modifier.width(TitleControlsGap))
}

/**
 * The expand/collapse button. With [showHint] it carries a small dot and moves once, to signal that
 * actions are inside. Its symbol is told whether the message is expanded, so it can show that.
 *
 * @param expanded whether the message is currently expanded.
 * @param showHint whether to show the "there is more inside" hint.
 * @param nudgeHint whether to also make the hinting movement.
 * @param colors the colors of the card.
 * @param label what a screen reader announces for the button.
 * @param icon draws the symbol of the button. It receives whether the message is expanded.
 * @param onHintNudged called once the hinting movement has been made.
 * @param onClick called when the button is pressed.
 */
@Composable
private fun ExpandButton(
    expanded: Boolean,
    showHint: Boolean,
    nudgeHint: Boolean,
    colors: HollerColors,
    label: String,
    icon: @Composable (expanded: Boolean) -> Unit,
    onHintNudged: () -> Unit,
    onClick: () -> Unit,
) {
    val nudge = remember { Animatable(0f) }
    val currentOnHintNudged by rememberUpdatedState(onHintNudged)

    if (nudgeHint) {
        LaunchedEffect(Unit) {
            delay(HINT_DELAY_MILLIS.milliseconds)
            repeat(HINT_NUDGE_COUNT) {
                nudge.animateTo(1f, tween(HINT_NUDGE_MILLIS))
                nudge.animateTo(0f, tween(HINT_NUDGE_MILLIS))
            }

            currentOnHintNudged()
        }
    }

    Box {
        ControlButton(
            label = label,
            size = ControlSize,
            colors = colors,
            onClick = onClick,
            modifier = Modifier.graphicsLayer { translationY = nudge.value * HintNudgeDistance.toPx() },
        ) {
            icon(expanded)
        }

        if (showHint) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-5).dp, y = 5.dp)
                    .size(6.dp)
                    .background(colors.progress, CircleShape),
            )
        }
    }
}
