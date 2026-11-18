package io.github.mradzinski.holler.sample.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import io.github.mradzinski.holler.HollerIconRing

// Holler has no icons of its own: the app supplies the icon of each message, and its color. These four are
// the sample's, made from Material Icons. They show the two ways to fill the `icon` slot:
//
//  - `HollerIconRing { … }` draws a ring around an icon. Used for the check mark and the cross.
//  - Any composable works directly. Used for the info and warning icons, which have an outline of
//    their own and need no ring.

/** A green check mark inside a ring, for the `icon` slot of a success message. */
@Composable
internal fun SuccessIcon() {
    HollerIconRing(tint = SuccessGreen) { Icon(Icons.Default.Check, contentDescription = null) }
}

/** A red cross inside a ring, for the `icon` slot of an error message. */
@Composable
internal fun ErrorIcon() {
    HollerIconRing(tint = ErrorRed) { Icon(Icons.Default.Close, contentDescription = null) }
}

/** An amber warning triangle, placed in the `icon` slot of a warning message as it is. */
@Composable
internal fun WarningIcon() {
    Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber)
}

/** A blue information symbol, placed in the `icon` slot of an informational message as it is. */
@Composable
internal fun InfoIcon() {
    Icon(Icons.Default.Info, contentDescription = null, tint = InfoBlue)
}
