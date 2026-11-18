package io.github.mradzinski.holler

import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The colors of a message: one color per part of the card.
 *
 * By default they come from the app's Material theme, through [HollerDefaults.colors]. To give
 * messages a look of their own, create one with your own colors, or adjust the defaults with `copy`,
 * for example `HollerDefaults.colors().copy(progress = …)`.
 *
 * @param container background of the card.
 * @param title color of the title text. It is also the content color of the `icon` slot, so an icon
 * that sets no tint of its own is drawn in it.
 * @param details color of the long text.
 * @param controls color of the expand and close buttons.
 * @param border color of the thin outline around the card. Use [Color.Transparent] for none.
 * @param shadow color of the shadow under the card.
 * @param footerContainer background of the strip at the bottom that holds the countdown, and the
 * actions and the close button when they are placed there.
 * @param countdown color of the countdown text.
 * @param progress color of the filled part of the progress bar, and of the dot used by
 * [HollerActionsVisibility.ChevronHint].
 * @param progressTrack color of the unfilled part of the progress bar. Use [Color.Transparent] for none.
 */
@Immutable
public data class HollerColors(
    val container: Color,
    val title: Color,
    val details: Color,
    val controls: Color,
    val border: Color,
    val shadow: Color,
    val footerContainer: Color,
    val countdown: Color,
    val progress: Color,
    val progressTrack: Color,
)

/**
 * The colors of the message currently being drawn. Read it inside a message slot to color
 * custom content consistently, for example `LocalHollerColors.current.progress`.
 *
 * It can only be read: the message sets it. Outside a message there is no theme to read at this
 * point, so it holds the colors of Material's baseline light scheme.
 */
public val LocalHollerColors: CompositionLocal<HollerColors> get() = ProvidableHollerColors

/** The same local as [LocalHollerColors], in the form the card needs to set it. */
internal val ProvidableHollerColors: ProvidableCompositionLocal<HollerColors> = staticCompositionLocalOf {
    HollerDefaults.colors(lightColorScheme())
}
