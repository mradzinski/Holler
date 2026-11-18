package io.github.mradzinski.holler.internal

/**
 * What a screen reader announces for each of the card's own buttons. The app supplies the words, in
 * its own language: the library has none.
 *
 * @param close announced for the close button.
 * @param expand announced for the expand button while the message is collapsed.
 * @param collapse announced for the expand button while the message is expanded.
 */
internal data class ControlDescriptions(
    val close: String,
    val expand: String,
    val collapse: String,
)
