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
 * Longer than a message with the short duration (5 seconds) stays on screen, in milliseconds. Advancing
 * by it is enough to show that such a message's timer is held or stopped: a running one would have
 * closed the message. Every frame of an advance is drawn, so a test waits no longer than it must.
 */
internal const val PAST_SHORT_DURATION: Long = 7_000L

/**
 * Longer than the longest ready-made duration (10 seconds), in milliseconds. Advancing by it is enough
 * to show that a message has no timer at all.
 */
internal const val PAST_ANY_DURATION: Long = 12_000L

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
