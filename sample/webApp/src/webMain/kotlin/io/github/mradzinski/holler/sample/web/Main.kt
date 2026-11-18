package io.github.mradzinski.holler.sample.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import io.github.mradzinski.holler.sample.App
import io.github.mradzinski.holler.sample.BrowserHistory
import io.github.mradzinski.holler.sample.startSample

/** Starts the web sample (JS and Wasm): the shared app drawn into the page body, tied to the browser's Back button. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startSample()

    ComposeViewport {
        App(platformEffects = { BrowserHistory() })
    }
}
