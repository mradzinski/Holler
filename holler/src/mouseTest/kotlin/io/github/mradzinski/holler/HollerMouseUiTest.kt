package io.github.mradzinski.holler

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Width of the card under test, in dp. */
private const val MOUSE_TEST_CARD_WIDTH: Int = 360

/** How far the tests drag the card with the mouse: past the touch slop, well short of dismissing it. */
private const val MOUSE_TEST_DRAG: Float = 40f

/** How far below the message the tests move the mouse pointer to take it off the message. */
private const val POINTER_AWAY: Float = 400f

/** How long the tests give the card to react to the pointer, in milliseconds. */
private const val SETTLE_MILLIS: Long = 1_500L

/** The title of the card under test, whose drawn width shows the size the card is drawn at. */
private const val MOUSE_TEST_TITLE: String = "A title long enough to measure a small change in its width"

/**
 * Tests of what a mouse pointer does to a message. They run only where there is a mouse, on desktop
 * and on the web.
 */
@OptIn(ExperimentalTestApi::class)
class HollerMouseUiTest {

    /** The reasons reported by the message under test, in order. */
    private val reasons = mutableListOf<HollerDismissReason>()

    @Test
    fun `with the option on a mouse pointer over the message pauses the timer until it leaves`() = runComposeUiTest {
        show(pauseOnPressOrHover = true)
        advanceTimeBy(1_100)
        onNodeWithText(MOUSE_TEST_TITLE).performMouseInput { moveTo(center) }

        advanceTimeBy(PAST_SHORT_DURATION)

        onNodeWithText("left:4").assertExists()
        reasons.shouldBeEmpty()

        onNodeWithText(MOUSE_TEST_TITLE).performMouseInput { moveTo(Offset(centerX, bottom + POINTER_AWAY)) }
        advanceTimeBy(3_000)

        reasons.shouldBeEmpty()

        advanceTimeBy(1_500)

        reasons shouldBe listOf(HollerDismissReason.Timeout)
    }

    @Test
    fun `letting go of a drag while the pointer is over the message keeps the timer paused`() = runComposeUiTest {
        show(pauseOnPressOrHover = true)
        advanceTimeBy(1_100)
        onNodeWithText(MOUSE_TEST_TITLE).performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(MOUSE_TEST_DRAG, 0f))
            release()
        }

        advanceTimeBy(PAST_SHORT_DURATION)

        reasons.shouldBeEmpty()

        onNodeWithText(MOUSE_TEST_TITLE).performMouseInput { moveTo(Offset(centerX, bottom + POINTER_AWAY)) }
        advanceTimeBy(5_000)

        reasons shouldBe listOf(HollerDismissReason.Timeout)
    }

    @Test
    fun `with the option off a mouse pointer over the message leaves the timer running`() = runComposeUiTest {
        show()
        advanceTimeBy(1_100)
        onNodeWithText(MOUSE_TEST_TITLE).performMouseInput { moveTo(center) }
        advanceTimeBy(1_000)

        onNodeWithText("left:3").assertExists()

        advanceTimeBy(4_000)

        reasons shouldBe listOf(HollerDismissReason.Timeout)
    }

    @Test
    fun `a mouse pointer over the card holds its timer but does not press it in`() = runComposeUiTest {
        show(pauseOnPressOrHover = true)

        val fullWidth = titleWidth()

        onNodeWithText(MOUSE_TEST_TITLE).performMouseInput { moveTo(center) }
        advanceTimeBy(SETTLE_MILLIS)

        titleWidth() shouldBe (fullWidth plusOrMinus 0.5f)
    }

    /**
     * The width the title is drawn at. The title is inside the card, so it is drawn smaller or larger
     * with it.
     *
     * @return the drawn width of the title, in pixels.
     */
    private fun ComposeUiTest.titleWidth(): Float = onNodeWithText(MOUSE_TEST_TITLE)
        .fetchSemanticsNode()
        .boundsInRoot
        .width

    /**
     * Shows one message that closes after the short duration, with a countdown the tests can read and
     * the test clock under manual control.
     *
     * @param pauseOnPressOrHover whether pressing or hovering over the message pauses its timer.
     */
    private fun ComposeUiTest.show(pauseOnPressOrHover: Boolean = false) {
        mainClock.autoAdvance = false
        setContent {
            Column(Modifier.width(MOUSE_TEST_CARD_WIDTH.dp)) {
                TestCard(
                    title = MOUSE_TEST_TITLE,
                    onDismiss = { reasons += it },
                    countdown = { Text("left:${it.inWholeSeconds}") },
                    duration = HollerDuration.Short,
                    pauseOnPressOrHover = pauseOnPressOrHover,
                )
            }
        }

        advanceTimeByFrame()
    }
}
