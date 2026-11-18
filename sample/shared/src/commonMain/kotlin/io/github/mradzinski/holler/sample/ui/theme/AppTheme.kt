package io.github.mradzinski.holler.sample.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.mradzinski.holler.HollerColors
import io.github.mradzinski.holler.HollerDefaults
import io.github.mradzinski.holler.HollerStyle

/** The green of the success icon. */
internal val SuccessGreen: Color = Color(0xFF3BB273)

/** The red of the error icon. */
internal val ErrorRed: Color = Color(0xFFE5484D)

/** The amber of the warning icon. */
internal val WarningAmber: Color = Color(0xFFF5A524)

/** The blue of the information icon. */
internal val InfoBlue: Color = Color(0xFF3B82F6)

/**
 * The sample's Material theme: Material's baseline light or dark colors, with its default typography
 * and shapes. Messages take their default look from it.
 *
 * @param dark whether to use the dark colors.
 * @param content the themed content.
 */
@Composable
internal fun SampleTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) {
            darkColorScheme()
        } else {
            lightColorScheme()
        },
        content = content,
    )
}

/** The looks the app can give its messages, chosen on the settings screen. */
internal enum class MessageTheme {
    /** Colors, typeface and shape of the app's Material theme: what messages use with no setup. */
    App,

    /** A custom look: indigo card, amber accents, squarer corners, its own typeface, a hard shadow. */
    Brand,
}

/**
 * The colors of a message for one of the app's looks.
 *
 * @param theme the look.
 * @return the colors.
 */
@Composable
internal fun messageColors(theme: MessageTheme): HollerColors = when (theme) {
    MessageTheme.App -> HollerDefaults.colors()
    MessageTheme.Brand -> brandColors()
}

/**
 * The shapes, text styles and shadow of a message for one of the app's looks.
 *
 * @param theme the look.
 * @return the style.
 */
@Composable
internal fun messageStyle(theme: MessageTheme): HollerStyle = when (theme) {
    MessageTheme.App -> HollerDefaults.style()
    MessageTheme.Brand -> brandStyle()
}
