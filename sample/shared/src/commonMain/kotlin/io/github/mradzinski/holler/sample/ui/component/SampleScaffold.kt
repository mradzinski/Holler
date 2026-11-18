package io.github.mradzinski.holler.sample.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import io.github.mradzinski.holler.sample.resources.Res
import io.github.mradzinski.holler.sample.resources.action_back
import org.jetbrains.compose.resources.stringResource

/**
 * The frame every screen of the sample uses: a Material top app bar over the screen's content. The bar
 * has a title, a Back arrow when the screen can be closed, and the screen's own buttons at the end. As
 * Material asks, it takes a tinted background once the content scrolls under it. While the app is
 * offline, a strip under the bar says so.
 *
 * @param title the title of the screen, on one line. An empty title leaves the bar to its buttons.
 * @param modifier applied to the frame.
 * @param onBack called when the Back arrow is pressed; `null` for a screen with no Back arrow.
 * @param actions the buttons at the end of the bar.
 * @param bottomBar what stays at the bottom of the screen, under the content.
 * @param content the content of the screen. It must apply the padding it is given, which keeps it clear
 *   of the bars.
 */
// Material's top app bar is still marked experimental. It is used here, in one place, for the whole sample.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SampleScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(Res.string.action_back),
                                )
                            }
                        }
                    },
                    actions = actions,
                    scrollBehavior = scrollBehavior,
                )

                OfflineStrip(visible = LocalIsOffline.current)
            }
        },
        bottomBar = bottomBar,
        content = content,
        // Tells the bar how far the content has scrolled, which is what tints it.
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    )
}
