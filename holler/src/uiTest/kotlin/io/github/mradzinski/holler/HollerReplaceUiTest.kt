package io.github.mradzinski.holler

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

/** The key the tests give the message they replace. */
private const val TEST_KEY: String = "upload"

/** Tests of replacing and closing a message by its key while a host draws it. */
@OptIn(ExperimentalTestApi::class)
class HollerReplaceUiTest : UiTest() {

    /** The reasons reported by the messages under test, in order. */
    private val reasons = mutableListOf<HollerDismissReason>()

    @Test
    fun `a visible message takes new content in the same card with no exit or enter animation`() = runComposeUiTest {
        val state = Holler()

        host(state)
        state
            .show(
                title = "Uploading",
                duration = HollerDuration.Infinite,
                key = TEST_KEY,
            )
            .reportTo(reasons)
        advanceTimeBy(1_000)

        state.show(title = "Uploaded", duration = HollerDuration.Infinite, key = TEST_KEY)
        advanceTimeByFrame()

        val visibility = state.entries.single().visibility

        onNodeWithText("Uploading").assertDoesNotExist()
        onNodeWithText("Uploaded").assertExists()
        visibility.shouldHaveEntered()
        reasons shouldBe listOf(HollerDismissReason.Replaced)
    }

    @Test
    fun `the timer of a replaced message starts again from the new duration`() = runComposeUiTest {
        val state = Holler()

        host(state)
        state.show(
            title = "First",
            duration = HollerDuration.Custom(3.seconds),
            key = TEST_KEY,
        )
        advanceTimeBy(2_000)

        state
            .show(
                title = "Second",
                duration = HollerDuration.Custom(3.seconds),
                key = TEST_KEY,
            )
            .reportTo(reasons)
        advanceTimeBy(2_000)

        onNodeWithText("Second").assertExists()
        reasons.shouldBeEmpty()

        advanceTimeBy(3_000)

        onNodeWithText("Second").assertDoesNotExist()
        reasons shouldBe listOf(HollerDismissReason.Timeout)
        state.entries.shouldBeEmpty()
    }

    @Test
    fun `an expanded message is collapsed and counting again after a replace`() = runComposeUiTest {
        val state = Holler()

        host(state)
        state.show(title = "First", details = "First details", key = TEST_KEY)
        advanceTimeBy(1_000)
        // A touch, so that no mouse pointer is left over the card, where it would hold the timer.
        onNodeWithContentDescription(EXPAND_DESCRIPTION).performTouchInput { click() }
        advanceTimeBy(1_000)

        onNodeWithContentDescription(COLLAPSE_DESCRIPTION).assertExists()

        advanceTimeBy(PAST_SHORT_DURATION)

        onNodeWithText("First").assertExists()

        state.show(title = "Second", details = "Second details", key = TEST_KEY)
        advanceTimeBy(1_000)

        onNodeWithContentDescription(EXPAND_DESCRIPTION).assertExists()

        advanceTimeBy(6_000)

        onNodeWithText("Second").assertDoesNotExist()
    }

    @Test
    fun `showing the same text again under the same key still starts the card and its timer over`() = runComposeUiTest {
        val state = Holler()

        host(state)
        state.show(title = "Saving", duration = HollerDuration.Custom(3.seconds), key = TEST_KEY)
        advanceTimeBy(2_000)

        state.show(title = "Saving", duration = HollerDuration.Custom(3.seconds), key = TEST_KEY)
        advanceTimeBy(2_000)

        onNodeWithText("Saving").assertExists()

        advanceTimeBy(3_000)

        onNodeWithText("Saving").assertDoesNotExist()
        state.entries.shouldBeEmpty()
    }

    @Test
    fun `closing by key plays the exit of a visible message and frees its key at once`() = runComposeUiTest {
        val state = Holler()

        host(state)
        state
            .show(
                title = "Old",
                duration = HollerDuration.Infinite,
                key = TEST_KEY,
            )
            .reportTo(reasons)
        advanceTimeBy(1_000)

        state.dismiss(TEST_KEY)
        state.show(title = "New", duration = HollerDuration.Infinite, key = TEST_KEY)

        state.entries shouldHaveSize 2

        advanceTimeBy(1_500)

        onNodeWithText("Old").assertDoesNotExist()
        onNodeWithText("New").assertExists()
        reasons shouldBe listOf(HollerDismissReason.Programmatic)
        state.entries shouldHaveSize 1
    }

    /**
     * Puts a host on screen with the test clock under manual control.
     *
     * @param state the state whose messages are drawn.
     */
    private fun ComposeUiTest.host(state: Holler) {
        mainClock.autoAdvance = false
        setContent {
            Box(Modifier.fillMaxSize()) {
                HollerHost(
                    holler = state,
                    closeContentDescription = CLOSE_DESCRIPTION,
                    expandContentDescription = EXPAND_DESCRIPTION,
                    collapseContentDescription = COLLAPSE_DESCRIPTION,
                )
            }
        }
    }
}
