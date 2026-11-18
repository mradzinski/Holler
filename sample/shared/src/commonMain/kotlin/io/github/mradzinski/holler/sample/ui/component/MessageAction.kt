package io.github.mradzinski.holler.sample.ui.component

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.mradzinski.holler.LocalHollerColors
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * An action button of a message, whose label is a string resource. It is a plain Material outlined
 * button: the `actions` slot of a message takes any button the app has. It uses Material's
 * rounded-rectangle ("square") button shape in place of the default pill, which sits better inside
 * a card. Its label uses the accent color of the message it is in, so it stays readable in a custom
 * look too.
 *
 * @param text the label.
 * @param formatArgs the values for the placeholders of [text], if it has any.
 * @param onClick called when the button is pressed.
 */
@Composable
internal fun MessageAction(
    text: StringResource,
    vararg formatArgs: Any,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        // The theme's medium shape is what Material uses for a square button of this size. Material 3 has
        // it ready-made as ButtonDefaults.squareShape, but not yet in a stable Compose Multiplatform release.
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalHollerColors.current.progress),
    ) {
        Text(
            text = stringResource(text, *formatArgs),
        )
    }
}
