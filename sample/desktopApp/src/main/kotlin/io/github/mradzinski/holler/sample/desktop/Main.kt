package io.github.mradzinski.holler.sample.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.mradzinski.holler.sample.App
import io.github.mradzinski.holler.sample.resources.Res
import io.github.mradzinski.holler.sample.resources.app_name
import io.github.mradzinski.holler.sample.startSample
import org.jetbrains.compose.resources.stringResource

/** Starts the desktop sample: one window showing the shared app. */
fun main() {
    startSample()

    application { SampleWindow() }
}

/** The one window of the desktop sample. Closing it ends the app. */
@Composable
private fun ApplicationScope.SampleWindow() {
    Window(
        onCloseRequest = ::exitApplication,
        title = stringResource(Res.string.app_name),
        state = rememberWindowState(width = 960.dp, height = 820.dp),
    ) {
        App()
    }
}
