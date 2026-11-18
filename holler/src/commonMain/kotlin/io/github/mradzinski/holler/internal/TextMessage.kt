package io.github.mradzinski.holler.internal

import io.github.mradzinski.holler.HollerDuration

/**
 * A message shown with `show(title, …)`: plain text, with no look of its own. It travels through the
 * queue like any value, but the host draws it itself, with its own look, and never hands it to the
 * app's `message` lambda.
 *
 * @param title the short message.
 * @param details the long text, or `null`.
 * @param duration the time on screen.
 */
internal data class TextMessage(
    val title: String,
    val details: String?,
    val duration: HollerDuration,
)
