package io.github.mradzinski.holler.internal

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle

/**
 * Makes [style] the text style of [content], and its color the content color: the color of [style]
 * when it has one, [fallbackColor] otherwise. They are set as Material's `LocalTextStyle` and
 * `LocalContentColor`, so a Material text or icon placed in the slot follows them by itself.
 *
 * The fallback color is deliberately not written into the text style. A Material button keeps the
 * surrounding text style for its label, so a color there would override the button's own label color.
 *
 * @param style the text style of the slot.
 * @param fallbackColor content color used when [style] does not specify a color.
 * @param content the slot content.
 */
@Composable
internal fun ProvideHollerTextStyle(
    style: TextStyle,
    fallbackColor: Color,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalTextStyle provides style,
        LocalContentColor provides style.color.takeOrElse { fallbackColor },
        content = content,
    )
}

/**
 * Turns optional plain text into an optional slot that draws it with a Material text.
 *
 * @param text the text, or `null`.
 * @return a slot drawing [text], or `null` when there is no text.
 */
internal fun textSlot(text: String?): (@Composable () -> Unit)? = if (text == null) {
    null
} else {
    { Text(text) }
}
