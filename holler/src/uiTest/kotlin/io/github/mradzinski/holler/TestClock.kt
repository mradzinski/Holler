package io.github.mradzinski.holler

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi

// The tests drive the clock by hand, and they change state from the test body: they show a message,
// close one, flip a value a message reads. An app does the same from its own code, and Compose learns
// of it before the next frame. A test clock is stricter: on Android it only notices such a change one
// call later, so a single long advance would run without it. These two functions tell Compose first,
// so every platform advances from the same state. The tests use them in place of `mainClock`'s own.

/**
 * Advances the test clock, after telling Compose about any state the test body has changed.
 *
 * @param milliseconds how far to advance the clock.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.advanceTimeBy(milliseconds: Long) {
    Snapshot.sendApplyNotifications()
    mainClock.advanceTimeBy(milliseconds)
}

/** Advances the test clock by one frame, after telling Compose about any state the test body has changed. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.advanceTimeByFrame() {
    Snapshot.sendApplyNotifications()
    mainClock.advanceTimeByFrame()
}
