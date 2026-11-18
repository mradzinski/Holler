package io.github.mradzinski.holler

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Width of the cards under test, in dp. Narrow enough to be a real phone width. */
private const val CARD_WIDTH: Int = 360

/** Height of a collapsed card with nothing under its top row, in dp. */
private const val COLLAPSED_HEIGHT: Int = 60

/** Tag of the card under test. */
private const val CARD_TAG: String = "card"

/**
 * Tests of where a message puts its actions and its close button, of the symbols a host gives the
 * card's own buttons, and of closing a message without its close button.
 */
@OptIn(ExperimentalTestApi::class)
class HollerPositionsUiTest : UiTest() {

    /** The reasons reported by the message under test, in order. */
    private val reasons = mutableListOf<HollerDismissReason>()

    @Test
    fun `actions at the top sit in the top row and are visible while the message is collapsed`() = runComposeUiTest {
        show(
            details = "Details",
            actionsPosition = HollerActionsPosition.Top,
            visibility = HollerActionsVisibility.ChevronHint,
        )

        val title = onNodeWithText("Saved").fetchSemanticsNode().boundsInRoot
        val action = onNodeWithText("Okay").fetchSemanticsNode().boundsInRoot
        val expand = onNodeWithContentDescription(EXPAND_DESCRIPTION).fetchSemanticsNode().boundsInRoot

        onNodeWithText("Details").assertDoesNotExist()
        withClue("the action must come after the title") {
            action.left shouldBeGreaterThanOrEqualTo title.right
        }
        withClue("the action must come before the expand button") {
            action.right shouldBeLessThanOrEqualTo expand.left
        }
        action.center.y shouldBe (expand.center.y plusOrMinus 1f)
    }

    @Test
    fun `a tall button at the top does not make the card taller and still takes a click`() = runComposeUiTest {
        show(duration = HollerDuration.Infinite, actionsPosition = HollerActionsPosition.Top)

        val card = onNodeWithTag(CARD_TAG).fetchSemanticsNode().size

        card.height shouldBe COLLAPSED_HEIGHT

        onNodeWithText("Okay").performTouchInput { click(Offset(centerX, 1f)) }

        reasons shouldBe listOf(HollerDismissReason.Action)
    }

    @Test
    fun `actions at the top do not start the message expanded`() = runComposeUiTest {
        show(
            details = "Details",
            actionsPosition = HollerActionsPosition.Top,
            visibility = HollerActionsVisibility.StartExpanded,
        )

        onNodeWithText("Okay").assertExists()
        onNodeWithText("Details").assertDoesNotExist()
    }

    @Test
    fun `bottom actions sit in the strip after the countdown and stay when the timer stops`() = runComposeUiTest {
        show(details = "Details", actionsPosition = HollerActionsPosition.Bottom)

        val title = onNodeWithText("Saved").fetchSemanticsNode().boundsInRoot
        val countdown = onNodeWithText("left:5").fetchSemanticsNode().boundsInRoot
        val action = onNodeWithText("Okay").fetchSemanticsNode().boundsInRoot

        withClue("the action must be below the title") {
            action.top shouldBeGreaterThanOrEqualTo title.bottom
        }
        withClue("the action must come after the countdown") {
            action.left shouldBeGreaterThanOrEqualTo countdown.right
        }

        onNodeWithContentDescription(EXPAND_DESCRIPTION).performClick()
        advanceTimeBy(2_000)

        onNodeWithText("left:", substring = true).assertDoesNotExist()
        onNodeWithText("Okay").performClick()

        reasons shouldBe listOf(HollerDismissReason.Action)
    }

    @Test
    fun `a message without a timer has the strip too when its actions are at the bottom`() = runComposeUiTest {
        show(duration = HollerDuration.Infinite, actionsPosition = HollerActionsPosition.Bottom)

        val title = onNodeWithText("Saved").fetchSemanticsNode().boundsInRoot
        val action = onNodeWithText("Okay").fetchSemanticsNode().boundsInRoot

        withClue("the action must be below the title") {
            action.top shouldBeGreaterThanOrEqualTo title.bottom
        }
    }

    @Test
    fun `a close button at the bottom sits at the end of the strip and reports the same reason`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            Column(Modifier.width(CARD_WIDTH.dp)) {
                TestCard(title = "Reference", onDismiss = {}, closeContentDescription = "Close at the top")
                TestCard(
                    title = "Saved",
                    onDismiss = { reasons += it },
                    actions = { TextButton(onClick = { dismiss() }) { Text("Okay") } },
                    countdown = { Text("left:${it.inWholeSeconds}") },
                    actionsPosition = HollerActionsPosition.Bottom,
                    closePosition = HollerClosePosition.Bottom,
                )
            }
        }

        advanceTimeByFrame()

        val topClose = onNodeWithContentDescription("Close at the top").fetchSemanticsNode().boundsInRoot
        val title = onNodeWithText("Saved").fetchSemanticsNode().boundsInRoot
        val action = onNodeWithText("Okay").fetchSemanticsNode().boundsInRoot
        val close = onNodeWithContentDescription(CLOSE_DESCRIPTION).fetchSemanticsNode().boundsInRoot

        withClue("the close button must be below the title") {
            close.top shouldBeGreaterThanOrEqualTo title.bottom
        }
        withClue("the close button must come after the actions") {
            close.left shouldBeGreaterThanOrEqualTo action.right
        }
        close.center.x shouldBe (topClose.center.x plusOrMinus 1f)

        onNodeWithContentDescription(CLOSE_DESCRIPTION).performClick()

        reasons shouldBe listOf(HollerDismissReason.CloseButton)
    }

    @Test
    fun `a close button at the bottom keeps the strip on a message that has no timer`() = runComposeUiTest {
        show(
            duration = HollerDuration.Infinite,
            withActions = false,
            closePosition = HollerClosePosition.Bottom,
        )

        val title = onNodeWithText("Saved").fetchSemanticsNode().boundsInRoot
        val close = onNodeWithContentDescription(CLOSE_DESCRIPTION).fetchSemanticsNode().boundsInRoot

        withClue("the close button must be below the title") {
            close.top shouldBeGreaterThanOrEqualTo title.bottom
        }
    }

    @Test
    fun `a message without a close button can still be dismissed by assistive technology`() = runComposeUiTest {
        show(withActions = false, closePosition = HollerClosePosition.None)

        onNodeWithContentDescription(CLOSE_DESCRIPTION).assertDoesNotExist()

        onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)).performSemanticsAction(SemanticsActions.Dismiss)

        reasons shouldBe listOf(HollerDismissReason.Swipe)
    }

    @Test
    fun `a message with its close button offers the same dismiss action`() = runComposeUiTest {
        show(withActions = false)

        onNodeWithContentDescription(CLOSE_DESCRIPTION).assertExists()
        onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)).performSemanticsAction(SemanticsActions.Dismiss)

        reasons shouldBe listOf(HollerDismissReason.Swipe)
    }

    @Test
    fun `the host's symbols are drawn inside the card's buttons which keep their descriptions`() = runComposeUiTest {
        val holler = Holler()

        mainClock.autoAdvance = false
        setContent {
            Box(Modifier.fillMaxSize()) {
                HollerHost(
                    holler = holler,
                    closeContentDescription = CLOSE_DESCRIPTION,
                    expandContentDescription = EXPAND_DESCRIPTION,
                    collapseContentDescription = COLLAPSE_DESCRIPTION,
                    animation = HollerAnimation.None,
                    expandIcon = { expanded ->
                        Text(
                            text = if (expanded) {
                                "less"
                            } else {
                                "more"
                            },
                        )
                    },
                    closeIcon = { Text("shut") },
                )
            }
        }

        holler.show(title = "Saved", details = "Details")
        advanceTimeBy(500)

        onNodeWithText("more", useUnmergedTree = true).assertExists()
        onNodeWithText("shut", useUnmergedTree = true).assertExists()

        onNodeWithContentDescription(EXPAND_DESCRIPTION).performClick()
        advanceTimeBy(1_000)

        onNodeWithText("less", useUnmergedTree = true).assertExists()
        onNodeWithContentDescription(COLLAPSE_DESCRIPTION).assertExists()

        onNodeWithContentDescription(CLOSE_DESCRIPTION).performClick()
        advanceTimeBy(1_000)

        holler.messages.shouldBeEmpty()
    }

    /**
     * Shows one message with the test clock under manual control.
     *
     * @param details the long text, or `null`.
     * @param withActions whether to add an action labeled "Okay" that dismisses the message.
     * @param duration the time on screen.
     * @param actionsPosition where the actions are placed.
     * @param visibility how actions in the middle are signaled.
     * @param closePosition where the close button is placed, or that there is none.
     */
    private fun ComposeUiTest.show(
        details: String? = null,
        withActions: Boolean = true,
        duration: HollerDuration = HollerDuration.Short,
        actionsPosition: HollerActionsPosition = HollerActionsPosition.Middle,
        visibility: HollerActionsVisibility = HollerActionsVisibility.AlwaysVisible,
        closePosition: HollerClosePosition = HollerClosePosition.Top,
    ) {
        mainClock.autoAdvance = false
        setContent {
            Column(Modifier.width(CARD_WIDTH.dp)) {
                TestCard(
                    title = "Saved",
                    onDismiss = { reasons += it },
                    modifier = Modifier.testTag(CARD_TAG),
                    details = details,
                    actions = if (withActions) {
                        { TextButton(onClick = { dismiss() }) { Text("Okay") } }
                    } else {
                        null
                    },
                    countdown = { Text("left:${it.inWholeSeconds}") },
                    duration = duration,
                    actionsPosition = actionsPosition,
                    actionsVisibility = visibility,
                    closePosition = closePosition,
                )
            }
        }

        advanceTimeByFrame()
    }
}
