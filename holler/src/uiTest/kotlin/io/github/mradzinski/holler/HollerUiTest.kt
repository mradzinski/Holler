package io.github.mradzinski.holler

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import io.github.mradzinski.holler.internal.defaultDuration
import io.github.mradzinski.holler.internal.startsExpanded
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** How far the pause tests drag the card: past the touch slop, well short of dismissing it. */
private const val PAUSE_TEST_DRAG: Float = 40f

/** Tests of what a message shows and does, driven through a real composition with a test clock. */
@OptIn(ExperimentalTestApi::class)
class HollerUiTest : UiTest() {

    /** The reasons reported by the message under test, in order. */
    private val reasons = mutableListOf<HollerDismissReason>()

    @Test
    fun `without a long text there is no expand button - only the close button`() = runComposeUiTest {
        show()

        onNodeWithText("Saved").assertExists()
        onNodeWithContentDescription(EXPAND_DESCRIPTION).assertDoesNotExist()
        onNodeWithContentDescription(CLOSE_DESCRIPTION).assertExists()
    }

    @Test
    fun `an infinite message has no countdown and never times out`() = runComposeUiTest {
        show(duration = HollerDuration.Infinite)

        onNodeWithText("left:", substring = true).assertDoesNotExist()

        advanceTimeBy(60_000)

        reasons.shouldBeEmpty()
        onNodeWithContentDescription(CLOSE_DESCRIPTION).assertExists()
    }

    @Test
    fun `the message reports a timeout once its time has passed`() = runComposeUiTest {
        show()
        advanceTimeBy(4_000)

        reasons.shouldBeEmpty()

        advanceTimeBy(1_500)

        reasons shouldBe listOf(HollerDismissReason.Timeout)
    }

    @Test
    fun `without a progress bar the timer runs on delays and still times out`() = runComposeUiTest {
        show(showProgressBar = false)
        advanceTimeBy(5_500)

        reasons shouldBe listOf(HollerDismissReason.Timeout)
    }

    @Test
    fun `the countdown shows the seconds left and drops as time passes`() = runComposeUiTest {
        show()

        onNodeWithText("left:5").assertExists()

        advanceTimeBy(1_100)

        onNodeWithText("left:4").assertExists()

        advanceTimeBy(2_000)

        onNodeWithText("left:2").assertExists()
    }

    @Test
    fun `expanding reveals the long text and stops the timer for good`() = runComposeUiTest {
        show(details = "Details")

        onNodeWithText("Details").assertDoesNotExist()

        advanceTimeBy(1_000)
        onNodeWithContentDescription(EXPAND_DESCRIPTION).performClick()
        advanceTimeBy(10_000)

        onNodeWithText("Details").assertExists()
        onNodeWithContentDescription(COLLAPSE_DESCRIPTION).assertExists()
        onNodeWithText("left:", substring = true).assertDoesNotExist()
        reasons.shouldBeEmpty()

        onNodeWithContentDescription(COLLAPSE_DESCRIPTION).performClick()
        advanceTimeBy(10_000)

        onNodeWithText("Saved").assertExists()
        onNodeWithText("left:", substring = true).assertDoesNotExist()
        reasons.shouldBeEmpty()
    }

    @Test
    fun `a message nothing can stop still shows its countdown and times out`() = runComposeUiTest {
        show(stoppableBy = HollerStopTrigger.None)

        onNodeWithText("left:5").assertExists()

        advanceTimeBy(6_000)

        reasons shouldBe listOf(HollerDismissReason.Timeout)
    }

    @Test
    fun `expanding a message nothing can stop leaves the timer running`() = runComposeUiTest {
        show(details = "Details", stoppableBy = HollerStopTrigger.None)
        onNodeWithContentDescription(EXPAND_DESCRIPTION).performClick()
        advanceTimeBy(1_000)

        onNodeWithText("Details").assertExists()
        onNodeWithText("left:", substring = true).assertExists()

        advanceTimeBy(5_000)

        reasons shouldBe listOf(HollerDismissReason.Timeout)
    }

    @Test
    fun `by default the actions are visible while the long text is collapsed`() = runComposeUiTest {
        show(details = "Details", withActions = true)

        onNodeWithText("Okay").assertExists()
        onNodeWithText("Details").assertDoesNotExist()
    }

    @Test
    fun `with the chevron hint the actions stay hidden until the user expands`() = runComposeUiTest {
        show(
            details = "Details",
            withActions = true,
            visibility = HollerActionsVisibility.ChevronHint,
        )

        onNodeWithText("Okay").assertDoesNotExist()

        onNodeWithContentDescription(EXPAND_DESCRIPTION).performClick()
        advanceTimeBy(1_000)

        onNodeWithText("Okay").assertExists()
    }

    @Test
    fun `starting expanded shows the long text and actions at once and has no timer by default`() = runComposeUiTest {
        show(
            details = "Details",
            withActions = true,
            duration = null,
            visibility = HollerActionsVisibility.StartExpanded,
        )

        onNodeWithText("Details").assertExists()
        onNodeWithText("Okay").assertExists()
        onNodeWithText("left:", substring = true).assertDoesNotExist()

        advanceTimeBy(60_000)

        onNodeWithText("Saved").assertExists()
        reasons.shouldBeEmpty()
    }

    @Test
    fun `a message that starts expanded refuses a timer`() = runComposeUiTest {
        val failure = shouldThrow<IllegalArgumentException> {
            show(
                details = "Details",
                withActions = true,
                duration = HollerDuration.Short,
                visibility = HollerActionsVisibility.StartExpanded,
            )

            waitForIdle()
        }

        failure.message shouldContain "HollerDuration.Infinite"
    }

    @Test
    fun `a message nothing can stop may start expanded with a timer and closes when time is up`() = runComposeUiTest {
        show(
            details = "Details",
            withActions = true,
            duration = HollerDuration.Short,
            visibility = HollerActionsVisibility.StartExpanded,
            stoppableBy = HollerStopTrigger.None,
        )

        onNodeWithText("Details").assertExists()
        onNodeWithText("left:5").assertExists()

        advanceTimeBy(6_000)

        reasons shouldBe listOf(HollerDismissReason.Timeout)
    }

    @Test
    fun `asking to start expanded changes nothing for a message with no long text`() = runComposeUiTest {
        show(
            withActions = true,
            duration = null,
            visibility = HollerActionsVisibility.StartExpanded,
        )

        onNodeWithText("left:5").assertExists()

        advanceTimeBy(6_000)

        reasons shouldBe listOf(HollerDismissReason.Timeout)
    }

    @Test
    fun `with actions but no long text the actions are visible whatever the setting`() = runComposeUiTest {
        show(withActions = true, visibility = HollerActionsVisibility.ChevronHint)

        onNodeWithText("Okay").assertExists()
        onNodeWithContentDescription(EXPAND_DESCRIPTION).assertDoesNotExist()
    }

    @Test
    fun `the close button and an action each report their own reason`() = runComposeUiTest {
        show(withActions = true)
        onNodeWithContentDescription(CLOSE_DESCRIPTION).performClick()
        onNodeWithText("Okay").performClick()

        reasons shouldBe listOf(HollerDismissReason.CloseButton, HollerDismissReason.Action)
    }

    @Test
    fun `swiping the card sideways dismisses it`() = runComposeUiTest {
        show(duration = HollerDuration.Infinite)
        onNodeWithText("Saved").performTouchInput {
            swipeRight(
                startX = centerX,
                endX = centerX + 2_000f,
            )
        }

        advanceTimeBy(1_000)

        reasons shouldBe listOf(HollerDismissReason.Swipe)
    }

    @Test
    fun `swiping the card toward the start side dismisses it too`() = runComposeUiTest {
        show(duration = HollerDuration.Infinite)
        onNodeWithText("Saved").performTouchInput {
            swipeLeft(
                startX = centerX,
                endX = centerX - 2_000f,
            )
        }

        advanceTimeBy(1_000)

        reasons shouldBe listOf(HollerDismissReason.Swipe)
    }

    @Test
    fun `dragging the card pauses the timer and letting go without dismissing resumes it`() = runComposeUiTest {
        show()
        advanceTimeBy(1_100)

        onNodeWithText("left:4").assertExists()

        onNodeWithText("Saved").performTouchInput {
            down(center)
            moveBy(Offset(PAUSE_TEST_DRAG, 0f))
        }

        advanceTimeBy(20_000)

        onNodeWithText("left:4").assertExists()
        reasons.shouldBeEmpty()

        onNodeWithText("Saved").performTouchInput { up() }
        advanceTimeBy(3_000)

        reasons.shouldBeEmpty()

        advanceTimeBy(1_500)

        reasons shouldBe listOf(HollerDismissReason.Timeout)
    }

    @Test
    fun `dragging pauses the timer without a progress bar too`() = runComposeUiTest {
        show(showProgressBar = false)
        advanceTimeBy(1_100)
        onNodeWithText("Saved").performTouchInput {
            down(center)
            moveBy(Offset(PAUSE_TEST_DRAG, 0f))
        }

        advanceTimeBy(20_000)

        reasons.shouldBeEmpty()

        onNodeWithText("Saved").performTouchInput { up() }
        advanceTimeBy(6_000)

        reasons shouldBe listOf(HollerDismissReason.Timeout)
    }

    @Test
    fun `a drag that ends in a dismissal reports the swipe only and never times out`() = runComposeUiTest {
        show()
        advanceTimeBy(1_000)
        onNodeWithText("Saved").performTouchInput {
            swipeRight(
                startX = centerX,
                endX = centerX + 2_000f,
            )
        }

        advanceTimeBy(20_000)

        reasons shouldBe listOf(HollerDismissReason.Swipe)
    }

    @Test
    fun `the host shows at most the allowed number of messages and promotes queued ones`() = runComposeUiTest {
        val state = Holler()

        mainClock.autoAdvance = false
        setContent {
            Box(Modifier.fillMaxSize()) {
                HollerHost(
                    holler = state,
                    closeContentDescription = CLOSE_DESCRIPTION,
                    expandContentDescription = EXPAND_DESCRIPTION,
                    collapseContentDescription = COLLAPSE_DESCRIPTION,
                    maxVisible = 1,
                )
            }
        }

        val first = state.show(title = "First", duration = HollerDuration.Infinite)

        state.show(
            title = "Second",
            duration = HollerDuration.Custom(2.seconds),
        )
        advanceTimeBy(1_000)

        onNodeWithText("First").assertExists()
        onNodeWithText("Second").assertDoesNotExist()

        first.dismiss()
        advanceTimeBy(1_000)

        onNodeWithText("First").assertDoesNotExist()
        onNodeWithText("Second").assertExists()

        advanceTimeBy(3_000)

        onNodeWithText("Second").assertDoesNotExist()
        state.entries.shouldBeEmpty()
    }

    @Test
    fun `with the option on a finger held on the message pauses the timer until it is lifted`() = runComposeUiTest {
        show(pauseOnPressOrHover = true)
        advanceTimeBy(1_100)
        onNodeWithText("Saved").performTouchInput { down(center) }

        advanceTimeBy(20_000)

        onNodeWithText("left:4").assertExists()
        reasons.shouldBeEmpty()

        onNodeWithText("Saved").performTouchInput { up() }
        advanceTimeBy(3_000)

        reasons.shouldBeEmpty()

        advanceTimeBy(1_500)

        reasons shouldBe listOf(HollerDismissReason.Timeout)
    }

    @Test
    fun `with the option off a press leaves the timer running but dragging pauses it`() = runComposeUiTest {
        show()
        advanceTimeBy(1_100)
        onNodeWithText("Saved").performTouchInput { down(center) }
        advanceTimeBy(1_000)

        onNodeWithText("left:3").assertExists()

        onNodeWithText("Saved").performTouchInput { moveBy(Offset(PAUSE_TEST_DRAG, 0f)) }
        advanceTimeBy(20_000)

        onNodeWithText("left:3").assertExists()
        reasons.shouldBeEmpty()
    }

    /**
     * A countdown that prints the seconds left, so tests can read it.
     *
     * @param remaining the time left.
     */
    @Composable
    private fun TestCountdown(remaining: Duration) {
        Text("left:${remaining.inWholeSeconds}")
    }

    /**
     * Shows one message with the test clock under manual control.
     *
     * @param details the long text, or `null`.
     * @param withActions whether to add an action labeled "Okay" that dismisses the message.
     * @param duration the time on screen, or `null` for the one a message gets when it sets none.
     * @param visibility how actions are signalled.
     * @param showProgressBar whether the progress bar is drawn.
     * @param stoppableBy what the user can do to stop the timer for good.
     * @param pauseOnPressOrHover whether pressing or hovering the message pauses the timer.
     */
    private fun ComposeUiTest.show(
        details: String? = null,
        withActions: Boolean = false,
        duration: HollerDuration? = HollerDuration.Short,
        visibility: HollerActionsVisibility = HollerActionsVisibility.AlwaysVisible,
        showProgressBar: Boolean = true,
        stoppableBy: HollerStopTrigger = HollerStopTrigger.Expand,
        pauseOnPressOrHover: Boolean = false,
    ) {
        mainClock.autoAdvance = false
        setContent {
            TestCard(
                title = "Saved",
                onDismiss = { reasons += it },
                closeContentDescription = CLOSE_DESCRIPTION,
                expandContentDescription = EXPAND_DESCRIPTION,
                collapseContentDescription = COLLAPSE_DESCRIPTION,
                details = details,
                actions = if (withActions) {
                    { TextButton(onClick = { dismiss() }) { Text("Okay") } }
                } else {
                    null
                },
                countdown = { TestCountdown(it) },
                actionsVisibility = visibility,
                showProgressBar = showProgressBar,
                stoppableBy = stoppableBy,
                duration = duration ?: defaultDuration(
                    startsExpanded = startsExpanded(
                        hasDetails = details != null,
                        hasActions = withActions,
                        actionsPosition = HollerActionsPosition.Middle,
                        actionsVisibility = visibility,
                    ),
                    stoppableBy = stoppableBy,
                ),
                pauseOnPressOrHover = pauseOnPressOrHover,
            )
        }

        advanceTimeByFrame()
    }
}
