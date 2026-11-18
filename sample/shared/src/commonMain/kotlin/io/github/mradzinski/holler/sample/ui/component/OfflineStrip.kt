package io.github.mradzinski.holler.sample.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mradzinski.holler.sample.resources.Res
import io.github.mradzinski.holler.sample.resources.offline_strip
import org.jetbrains.compose.resources.stringResource

/** Space above and below the text of the offline strip. */
private val StripPadding: Dp = 4.dp

/**
 * Whether the app is offline. The app's root provides it, and every screen's frame reads it to draw
 * the [OfflineStrip], so no screen has to carry it in its own state.
 */
internal val LocalIsOffline: ProvidableCompositionLocal<Boolean> = compositionLocalOf { false }

/**
 * A thin strip that says the app is offline, for as long as it is. Being offline is a lasting state,
 * not something that just happened, so it is part of the screen: it cannot be closed, and it is not a
 * Holler message. The message is only the short notice shown when the connection drops.
 *
 * @param visible whether the app is offline. The strip slides open and shut as this changes.
 * @param modifier applied to the strip.
 */
@Composable
internal fun OfflineStrip(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier,
    ) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest) {
            Text(
                text = stringResource(Res.string.offline_strip),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = StripPadding),
            )
        }
    }
}
