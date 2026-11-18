package io.github.mradzinski.holler

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Width of the card under test, in dp. */
private const val PRESS_TEST_CARD_WIDTH: Int = 360

/** The size of a pressed card, as a share of its full size. */
private const val PRESSED_SHARE: Float = 0.97f

/** How far over its full size a released card must at least grow for its bounce to count as visible. */
private const val VISIBLE_BOUNCE_SHARE: Float = 1.01f

/** How long the tests give a press, a release or a swipe to finish, in milliseconds. */
private const val SETTLE_MILLIS: Long = 1_500L

/** How many frames the tests watch a card for after letting it go. */
private const val WATCHED_FRAMES: Int = 60

/** How far the tests drag a card that should spring back: past the touch slop, short of dismissing. */
private const val SHORT_DRAG: Float = 40f

/** How far the tests drag a card that should be dismissed. */
private const val LONG_DRAG: Float = 300f

/** How long the tests hold a press that only needs the card to finish shrinking, in milliseconds. */
private const val PRESS_HOLD_MILLIS: Long = 500L

/** The value the tests show through a host. */
private const val TIMED_VALUE: Int = 1

/** The title of the card under test, whose drawn width shows the size the card is drawn at. */
private const val PRESS_TEST_TITLE: String = "A title long enough to measure a small change in its width"

/** Tests of how a card gives under a press: it shrinks while pressed and bounces back when let go. */
@OptIn(ExperimentalTestApi::class)
class HollerPressUiTest : UiTest() {

    /** The reasons reported by the message under test, in order. */
    private val reasons = mutableListOf<HollerDismissReason>()

    @Test
    fun `a pressed card is drawn smaller and bounces past its full size when let go`() = runComposeUiTest {
        show()

        val fullWidth = titleWidth()

        onNodeWithText(PRESS_TEST_TITLE).performTouchInput { down(center) }
        advanceTimeBy(SETTLE_MILLIS)

        titleWidth() shouldBe (fullWidth * PRESSED_SHARE plusOrMinus 1f)

        onNodeWithText(PRESS_TEST_TITLE).performTouchInput { up() }

        val widest = widestTitleWhileSettling()

        withClue("the card must bounce past its size, grew to $widest") {
            widest shouldBeGreaterThanOrEqualTo fullWidth * VISIBLE_BOUNCE_SHARE
        }
        titleWidth() shouldBe (fullWidth plusOrMinus 0.5f)
        reasons.shouldBeEmpty()
    }

    @Test
    fun `a press on one of the card's buttons does not shrink the card`() = runComposeUiTest {
        show()

        val fullWidth = titleWidth()

        onNodeWithText("Okay").performTouchInput { down(center) }
        advanceTimeBy(SETTLE_MILLIS)

        titleWidth() shouldBe (fullWidth plusOrMinus 0.5f)

        onNodeWithText("Okay").performTouchInput { up() }

        reasons shouldBe listOf(HollerDismissReason.Action)
    }

    @Test
    fun `a card stays pressed in for a whole drag and bounces back when the drag springs back`() = runComposeUiTest {
        show()

        val fullWidth = titleWidth()

        onNodeWithText(PRESS_TEST_TITLE).performTouchInput {
            down(center)
            moveBy(Offset(SHORT_DRAG, 0f))
        }

        advanceTimeBy(SETTLE_MILLIS)

        titleWidth() shouldBe (fullWidth * PRESSED_SHARE plusOrMinus 1f)

        onNodeWithText(PRESS_TEST_TITLE).performTouchInput { up() }

        val widest = widestTitleWhileSettling()

        withClue("the card must bounce past its size, grew to $widest") {
            widest shouldBeGreaterThanOrEqualTo fullWidth * VISIBLE_BOUNCE_SHARE
        }
        reasons.shouldBeEmpty()
    }

    @Test
    fun `a card that is swiped away returns to its full size without bouncing`() = runComposeUiTest {
        show()

        val fullWidth = titleWidth()

        onNodeWithText(PRESS_TEST_TITLE).performTouchInput {
            down(center)
            moveBy(Offset(SHORT_DRAG, 0f))
            moveBy(Offset(LONG_DRAG, 0f))
        }

        advanceTimeBy(SETTLE_MILLIS)
        onNodeWithText(PRESS_TEST_TITLE).performTouchInput { up() }

        val widest = widestTitleWhileSettling()

        withClue("a leaving card must not grow past its size, grew to $widest") {
            widest shouldBeLessThanOrEqualTo fullWidth + 0.5f
        }
        titleWidth() shouldBe (fullWidth plusOrMinus 0.5f)
        reasons shouldBe listOf(HollerDismissReason.Swipe)
    }

    @Test
    fun `a host can turn the press animation off for all its messages`() = runComposeUiTest {
        val holler = Holler()

        host(holler, animateOnPress = false)
        holler.show(TIMED_VALUE)
        advanceTimeBy(SETTLE_MILLIS)

        val fullWidth = titleWidth()

        onNodeWithText(PRESS_TEST_TITLE).performTouchInput { down(center) }
        advanceTimeBy(PRESS_HOLD_MILLIS)

        titleWidth() shouldBe (fullWidth plusOrMinus 0.5f)

        onNodeWithText(PRESS_TEST_TITLE).performTouchInput { up() }

        val widest = widestTitleWhileSettling()

        widest shouldBe (fullWidth plusOrMinus 0.5f)
    }

    @Test
    fun `a card whose time runs out while it is pressed leaves without bouncing`() = runComposeUiTest {
        val holler = Holler()

        host(holler)
        holler.show(TIMED_VALUE)
        advanceTimeBy(SETTLE_MILLIS)

        val fullWidth = titleWidth()

        onNodeWithText(PRESS_TEST_TITLE).performTouchInput { down(center) }

        // Hold the card until the very frame its time runs out, so it is let go just as it starts leaving.
        while (holler.messages.isNotEmpty()) advanceTimeByFrame()

        onNodeWithText(PRESS_TEST_TITLE).performTouchInput { up() }

        val widths = titleWidthsWhileLeaving()

        withClue("the card must still be on screen for some frames after it timed out") {
            widths.shouldNotBeEmpty()
        }
        withClue("a leaving card must not grow past its size, grew to ${widths.max()}") {
            widths.max() shouldBeLessThanOrEqualTo fullWidth + 0.5f
        }
        holler.messages.shouldBeEmpty()
    }

    @Test
    fun `a card closed from code while it is pressed does not bounce when let go`() = runComposeUiTest {
        val holler = Holler()

        host(holler)
        holler.show(TIMED_VALUE)
        advanceTimeBy(SETTLE_MILLIS)

        val fullWidth = titleWidth()

        onNodeWithText(PRESS_TEST_TITLE).performTouchInput { down(center) }
        advanceTimeBy(PRESS_HOLD_MILLIS)

        titleWidth() shouldBe (fullWidth * PRESSED_SHARE plusOrMinus 1f)

        holler.dismissAll()
        advanceTimeByFrame()
        onNodeWithText(PRESS_TEST_TITLE).performTouchInput { up() }

        val widths = titleWidthsWhileLeaving()

        withClue("the card must still be on screen for some frames after it was closed") {
            widths.shouldNotBeEmpty()
        }
        withClue("a leaving card must not grow past its size, grew to ${widths.max()}") {
            widths.max() shouldBeLessThanOrEqualTo fullWidth + 0.5f
        }
    }

    /**
     * The width the title is drawn at. The title is inside the card, so it is drawn smaller or larger
     * with it.
     *
     * @return the drawn width of the title, in pixels.
     */
    private fun ComposeUiTest.titleWidth(): Float = onNodeWithText(PRESS_TEST_TITLE)
        .fetchSemanticsNode()
        .boundsInRoot
        .width

    /**
     * Lets the card settle after it was let go, one frame at a time.
     *
     * @return the largest width the title was drawn at on the way.
     */
    private fun ComposeUiTest.widestTitleWhileSettling(): Float {
        var widest = 0f

        repeat(WATCHED_FRAMES) {
            advanceTimeByFrame()
            widest = maxOf(widest, titleWidth())
        }

        advanceTimeBy(SETTLE_MILLIS)

        return widest
    }

    /**
     * Watches a card that is leaving, one frame at a time, until it is gone.
     *
     * @return the width the title was drawn at on every frame the card was still on screen.
     */
    private fun ComposeUiTest.titleWidthsWhileLeaving(): List<Float> {
        val widths = mutableListOf<Float>()

        repeat(WATCHED_FRAMES) {
            advanceTimeByFrame()

            onAllNodesWithText(PRESS_TEST_TITLE)
                .fetchSemanticsNodes()
                .forEach { widths += it.boundsInRoot.width }
        }

        advanceTimeBy(SETTLE_MILLIS)

        return widths
    }

    /**
     * Puts a host on screen whose messages fade out, so a leaving card stays visible for a moment, and
     * whose one kind of message is not paused by a press, so it can time out under the finger.
     *
     * @param holler the object whose messages are drawn.
     * @param animateOnPress whether the messages give under a press.
     */
    private fun ComposeUiTest.host(holler: Holler, animateOnPress: Boolean = true) {
        mainClock.autoAdvance = false
        setContent {
            Box(Modifier.fillMaxSize()) {
                HollerHost(
                    holler = holler,
                    closeContentDescription = CLOSE_DESCRIPTION,
                    expandContentDescription = EXPAND_DESCRIPTION,
                    collapseContentDescription = COLLAPSE_DESCRIPTION,
                    animation = HollerAnimation.Fade,
                    animateOnPress = animateOnPress,
                ) {
                    HollerMessage(title = PRESS_TEST_TITLE, pauseOnPressOrHover = false)
                }
            }
        }
    }

    /**
     * Shows one message that never closes by itself, with the test clock under manual control.
     *
     * @param pauseOnPressOrHover whether pressing or hovering over the message pauses its timer.
     */
    private fun ComposeUiTest.show(pauseOnPressOrHover: Boolean = false) {
        mainClock.autoAdvance = false
        setContent {
            Column(Modifier.width(PRESS_TEST_CARD_WIDTH.dp)) {
                TestCard(
                    title = PRESS_TEST_TITLE,
                    onDismiss = { reasons += it },
                    actions = { TextButton(onClick = { dismiss() }) { Text("Okay") } },
                    duration = HollerDuration.Infinite,
                    pauseOnPressOrHover = pauseOnPressOrHover,
                )
            }
        }

        advanceTimeByFrame()
    }
}
