package io.github.mradzinski.holler.sample.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The diameter of an avatar, which is Material's size for the leading avatar of a list item. */
private val AvatarSize: Dp = 40.dp

/** Shown in the avatar of a sender with no name. */
private const val NO_INITIAL: String = "?"

/**
 * The avatar of an email's sender: a circle with the first letter of their name. Its color is one of
 * the theme's three container colors, picked from the name, so a sender keeps the same color everywhere.
 *
 * @param name the name of the sender.
 * @param modifier applied to the avatar.
 */
@Composable
internal fun SenderAvatar(name: String, modifier: Modifier = Modifier) {
    val (container, content) = avatarColors(name)

    Box(
        contentAlignment = Alignment.Center,
        // The name is always written next to the avatar, so a screen reader skips the letter.
        modifier = modifier
            .size(AvatarSize)
            .background(container, CircleShape)
            .clearAndSetSemantics {},
    ) {
        Text(
            text = initialOf(name),
            style = MaterialTheme.typography.titleMedium,
            color = content,
        )
    }
}

/**
 * The letter an avatar shows.
 *
 * @param name the name of the sender.
 * @return the first letter of the name in upper case, or a question mark for an empty name.
 */
internal fun initialOf(name: String): String = name
    .trim()
    .firstOrNull()
    ?.uppercase()
    ?: NO_INITIAL

/**
 * Picks the colors of an avatar from the sender's name.
 *
 * @param name the name of the sender.
 * @return the background color of the avatar and the color of its letter.
 */
@Composable
private fun avatarColors(name: String): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme

    val choices = listOf(
        scheme.primaryContainer to scheme.onPrimaryContainer,
        scheme.secondaryContainer to scheme.onSecondaryContainer,
        scheme.tertiaryContainer to scheme.onTertiaryContainer,
    )

    return choices[name.hashCode().mod(choices.size)]
}
