package io.github.mradzinski.holler.sample.ui.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import io.github.mradzinski.holler.sample.resources.Res
import io.github.mradzinski.holler.sample.resources.countdown
import kotlin.time.Duration
import org.jetbrains.compose.resources.pluralStringResource

/**
 * The countdown text of a message: "This message will close in N seconds.", with the number in bold.
 * It is a plain Material `Text`: inside the countdown slot it gets the countdown's style and color
 * by itself.
 *
 * @param remaining the time left before the message closes.
 */
@Composable
internal fun MessageCountdown(remaining: Duration) {
    val seconds = remaining.inWholeSeconds.toInt()
    val text = pluralStringResource(Res.plurals.countdown, seconds, seconds)

    // The sentence comes whole from the resources, so its word order can change with the language.
    // The number is then found in it and set in bold.
    val number = seconds.toString()
    val numberStart = text.indexOf(number)

    Text(
        buildAnnotatedString {
            append(text)

            if (numberStart >= 0) {
                addStyle(
                    style = SpanStyle(fontWeight = FontWeight.SemiBold),
                    start = numberStart,
                    end = numberStart + number.length,
                )
            }
        },
    )
}
