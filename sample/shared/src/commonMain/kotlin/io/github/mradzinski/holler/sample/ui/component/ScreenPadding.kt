package io.github.mradzinski.holler.sample.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection

// A screen's scaffold hands its content the padding that keeps it clear of the bars and of the system's
// insets. A scrolling screen splits that padding in two: the top and the sides go around the scrolling
// area, and the bottom goes inside it. The content then scrolls under the system's bottom inset (the
// home indicator, the navigation bar), and only its last item stops above it.

/**
 * The top and the sides of a screen's padding, for the outside of a scrolling area.
 *
 * @return this padding with no bottom.
 */
@Composable
internal fun PaddingValues.withoutBottom(): PaddingValues {
    val direction = LocalLayoutDirection.current

    return PaddingValues(
        start = calculateStartPadding(direction),
        top = calculateTopPadding(),
        end = calculateEndPadding(direction),
    )
}

/**
 * The bottom of a screen's padding, for the inside of a scrolling area.
 *
 * @return a padding with only this padding's bottom.
 */
internal fun PaddingValues.onlyBottom(): PaddingValues = PaddingValues(bottom = calculateBottomPadding())
