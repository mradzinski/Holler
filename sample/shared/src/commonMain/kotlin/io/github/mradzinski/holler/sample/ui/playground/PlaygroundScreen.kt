package io.github.mradzinski.holler.sample.ui.playground

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mradzinski.holler.Holler
import io.github.mradzinski.holler.sample.core.navigation.Navigator
import io.github.mradzinski.holler.sample.resources.Res
import io.github.mradzinski.holler.sample.resources.demo_action_top
import io.github.mradzinski.holler.sample.resources.demo_actions_bottom
import io.github.mradzinski.holler.sample.resources.demo_actions_visible
import io.github.mradzinski.holler.sample.resources.demo_close_bottom
import io.github.mradzinski.holler.sample.resources.demo_custom_look
import io.github.mradzinski.holler.sample.resources.demo_dismiss_all
import io.github.mradzinski.holler.sample.resources.demo_everything
import io.github.mradzinski.holler.sample.resources.demo_infinite
import io.github.mradzinski.holler.sample.resources.demo_no_close
import io.github.mradzinski.holler.sample.resources.demo_no_progress_bar
import io.github.mradzinski.holler.sample.resources.demo_replace
import io.github.mradzinski.holler.sample.resources.demo_starts_expanded
import io.github.mradzinski.holler.sample.resources.demo_text_only
import io.github.mradzinski.holler.sample.resources.demo_title_only
import io.github.mradzinski.holler.sample.resources.playground_title
import io.github.mradzinski.holler.sample.resources.text_only_details
import io.github.mradzinski.holler.sample.resources.text_only_title
import io.github.mradzinski.holler.sample.ui.component.SampleScaffold
import io.github.mradzinski.holler.sample.ui.component.onlyBottom
import io.github.mradzinski.holler.sample.ui.component.withoutBottom
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Space between the buttons of the playground, across and down. */
private val ButtonGap: Dp = 8.dp

/** Widest the two columns of buttons get, so they do not stretch across a large window. */
private val ButtonsMaxWidth: Dp = 560.dp

/**
 * The playground wired to the app's navigation.
 *
 * @param holler the host state of the app's `HollerHost`, which the buttons of the playground call.
 */
@Composable
internal fun PlaygroundRoute(holler: Holler) {
    val navigator = koinInject<Navigator>()

    PlaygroundScreen(
        holler = holler,
        onBack = { navigator.goBack() },
    )
}

/**
 * The playground: one button per kind of message, in two columns, to see everything Holler can do in
 * one place.
 *
 * Unlike the rest of the app, its buttons call Holler directly and there is no view model: the screen
 * is a catalog of the library, not an example of how to structure an app. For that, see how the inbox
 * and the email screens show their messages. The messages themselves are shown the same way as
 * everywhere else, as values the app's host gives a look; the one exception is the text-only demo,
 * which needs no look at all.
 *
 * @param holler the object the buttons show their messages through.
 * @param onBack called when the Back arrow is pressed.
 */
@Composable
internal fun PlaygroundScreen(holler: Holler, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()

    // A click handler cannot read string resources, so the text of the text-only demo is read here.
    val textOnlyTitle = stringResource(Res.string.text_only_title)
    val textOnlyDetails = stringResource(Res.string.text_only_details)

    SampleScaffold(
        title = stringResource(Res.string.playground_title),
        onBack = onBack,
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding.withoutBottom())
                .verticalScroll(rememberScrollState())
                .padding(innerPadding.onlyBottom())
                .padding(16.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(ButtonGap),
                modifier = Modifier.widthIn(max = ButtonsMaxWidth),
            ) {
                DemoRow {
                    DemoButton(Res.string.demo_everything) { holler.show(DemoMessage.Everything) }
                    DemoButton(Res.string.demo_text_only) { holler.show(title = textOnlyTitle, details = textOnlyDetails) }
                }

                DemoRow {
                    DemoButton(Res.string.demo_title_only) { holler.show(DemoMessage.TitleOnly) }
                    DemoButton(Res.string.demo_actions_visible) { holler.show(DemoMessage.ActionsAlwaysVisible) }
                }

                DemoRow {
                    DemoButton(Res.string.demo_starts_expanded) { holler.show(DemoMessage.StartsExpanded) }
                    DemoButton(Res.string.demo_infinite) { holler.show(DemoMessage.Infinite) }
                }

                DemoRow {
                    DemoButton(Res.string.demo_no_progress_bar) { holler.show(DemoMessage.WithoutProgressBar) }
                    DemoButton(Res.string.demo_replace) { holler.showUpload(scope) }
                }

                DemoRow {
                    DemoButton(Res.string.demo_action_top) { holler.show(DemoMessage.Position.ActionAtTop) }
                    DemoButton(Res.string.demo_actions_bottom) { holler.show(DemoMessage.Position.ActionsAtBottom) }
                }

                DemoRow {
                    DemoButton(Res.string.demo_close_bottom) { holler.show(DemoMessage.Position.CloseAtBottom) }
                    DemoButton(Res.string.demo_no_close) { holler.show(DemoMessage.Position.WithoutCloseButton) }
                }

                DemoRow {
                    DemoButton(Res.string.demo_custom_look) { holler.show(DemoMessage.CustomLook) }
                    DemoButton(Res.string.demo_dismiss_all) { holler.dismissAll() }
                }
            }
        }
    }
}

/**
 * One row of the playground's buttons. The buttons share its width equally and are as tall as the
 * tallest of them, so a label that wraps does not leave its neighbor shorter.
 *
 * @param buttons the buttons of the row, each a [DemoButton].
 */
@Composable
private fun DemoRow(buttons: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(ButtonGap),
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        content = buttons,
    )
}

/**
 * A Material button of the playground, which takes an equal share of its row.
 *
 * @param text the label.
 * @param formatArgs the values for the placeholders of [text], if it has any.
 * @param onClick called when the button is pressed.
 */
@Composable
private fun RowScope.DemoButton(
    text: StringResource,
    vararg formatArgs: Any,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
    ) {
        Text(
            text = stringResource(text, *formatArgs),
            textAlign = TextAlign.Center,
        )
    }
}
