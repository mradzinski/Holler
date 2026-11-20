package io.github.mradzinski.holler

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import io.github.mradzinski.holler.internal.CardState
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

/** Longer than the expand button takes to wait and make its hinting movement, in milliseconds. */
private const val PAST_HINT_MILLIS: Long = 1_600L

/** How far apart the tests look at the expand button while it may be moving, in milliseconds. */
private const val HINT_STEP_MILLIS: Long = 40L

/**
 * Tests of a card that is drawn, taken away and drawn again for the same content, as happens to a
 * message that goes under a pile and comes back: the second card goes on from what the first one kept.
 */
@OptIn(ExperimentalTestApi::class)
class HollerCardStateUiTest : UiTest() {

    /** The reasons reported by the card under test, in order. */
    private val reasons = mutableListOf<HollerDismissReason>()

    /** Whether the card under test is drawn. */
    private var drawn by mutableStateOf(true)

    @Test
    fun `the expand button makes its hinting movement once and not again when the card is drawn again`() = runComposeUiTest {
        card(actionsVisibility = HollerActionsVisibility.ChevronHint)

        withClue("the expand button must move the first time") {
            expandButtonTravel() shouldBeGreaterThan 0f
        }

        drawAgain()

        withClue("the expand button must stay still the second time") {
            expandButtonTravel() shouldBe 0f
        }
        onNodeWithContentDescription(EXPAND_DESCRIPTION).assertExists()
    }

    @Test
    fun `a card taken away before its hinting movement makes it when it is drawn again`() = runComposeUiTest {
        card(actionsVisibility = HollerActionsVisibility.ChevronHint)
        advanceTimeBy(HINT_STEP_MILLIS)

        drawAgain()

        expandButtonTravel() shouldBeGreaterThan 0f
    }

    @Test
    fun `a card drawn again goes on with the time it had left`() = runComposeUiTest {
        card(duration = HollerDuration.Custom(5.seconds))
        advanceTimeBy(2_200)

        onNodeWithText("left:3").assertExists()

        drawAgain()
        advanceTimeBy(300)

        onNodeWithText("left:3").assertExists()

        advanceTimeBy(3_000)

        reasons shouldBe listOf(HollerDismissReason.Timeout)
    }

    @Test
    fun `a card drawn again is still expanded and its timer still stopped`() = runComposeUiTest {
        card(duration = HollerDuration.Custom(5.seconds))
        onNodeWithContentDescription(EXPAND_DESCRIPTION).performTouchInput { click() }
        advanceTimeBy(500)

        drawAgain()
        advanceTimeBy(PAST_SHORT_DURATION)

        onNodeWithContentDescription(COLLAPSE_DESCRIPTION).assertExists()
        onNodeWithText("Details").assertExists()
        reasons.shouldBeEmpty()
    }

    /**
     * How far the expand button moves up and down from now until its hinting movement would be over.
     *
     * @return the distance between its highest and its lowest place, in pixels.
     */
    private fun ComposeUiTest.expandButtonTravel(): Float {
        val tops = mutableListOf<Float>()

        repeat((PAST_HINT_MILLIS / HINT_STEP_MILLIS).toInt()) {
            advanceTimeBy(HINT_STEP_MILLIS)

            tops += onNodeWithContentDescription(EXPAND_DESCRIPTION)
                .fetchSemanticsNode()
                .boundsInRoot
                .top
        }

        return tops.max() - tops.min()
    }

    /** Takes the card away and draws a new one for the same content. */
    private fun ComposeUiTest.drawAgain() {
        drawn = false
        advanceTimeByFrame()
        drawn = true
        advanceTimeByFrame()
    }

    /**
     * Puts a card on screen that can be taken away and drawn again, with the test clock under manual
     * control. The new card is given what the first one kept.
     *
     * @param duration the time on screen.
     * @param actionsVisibility how the presence of the card's action is signaled.
     */
    private fun ComposeUiTest.card(
        duration: HollerDuration = HollerDuration.Infinite,
        actionsVisibility: HollerActionsVisibility = HollerActionsVisibility.ChevronHint,
    ) {
        val state = CardState()

        mainClock.autoAdvance = false
        setContent {
            if (drawn) {
                TestCard(
                    title = "Title",
                    onDismiss = { reasons += it },
                    details = "Details",
                    actions = { TextButton(onClick = { dismiss() }) { Text("Okay") } },
                    countdown = { Text("left:${it.inWholeSeconds}") },
                    actionsVisibility = actionsVisibility,
                    duration = duration,
                    state = state,
                )
            }
        }
    }
}
