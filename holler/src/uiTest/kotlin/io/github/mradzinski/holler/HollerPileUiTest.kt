package io.github.mradzinski.holler

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import io.github.mradzinski.holler.internal.pileCardAlpha
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** How much smaller a card one step back in a pile is drawn, as a share of its size. */
private const val ONE_STEP_BACK_SHARE: Float = 0.95f

/** The size of a pressed front card, as a share of its full size. */
private const val PRESSED_FRONT_SHARE: Float = 0.97f

/** The size the card right behind a pressed front card follows to, as a share of its own size. */
private const val FOLLOWING_SHARE: Float = 0.985f

/** How long the tests give the pile to finish moving, in milliseconds. */
private const val PILE_SETTLE_MILLIS: Long = 1_000L

/** Half the time a pile takes to move, in milliseconds: a moment at which a moving height is on its way. */
private const val PILE_HALFWAY_MILLIS: Long = 100L

/** The name of a message whose title takes two lines, so its card is taller than the others. */
private const val TALL: String = "Tall\nmessage"

/** The name of a message whose title takes three lines, so its card is taller still. */
private const val TALLER: String = "Taller\nmessage\nstill"

/**
 * A message of the pile tests.
 *
 * @param name the title of the message, which also tells its cards apart.
 */
private data class PileMessage(val name: String)

/** Tests of messages arranged as a pile: the newest in front, the others behind it. */
@OptIn(ExperimentalTestApi::class)
class HollerPileUiTest : UiTest() {

    /** Where each card that was drawn is on screen, by the name of its message. */
    private val cards = mutableMapOf<String, LayoutCoordinates>()

    @Test
    fun `the newest message is in front and the ones behind it are hidden from a screen reader`() = runComposeUiTest {
        val holler = Holler()

        host(holler)
        show(holler, "First", "Second", "Third")

        bounds("Third").width shouldBe (CARD_FULL_WIDTH plusOrMinus 1f)
        withClue("the second message must be behind the third") {
            bounds("Second").width shouldBeLessThan bounds("Third").width
        }
        withClue("the first message must be behind the second") {
            bounds("First").width shouldBeLessThan bounds("Second").width
        }
        onNode(hasText("Third") and isHidden()).assertDoesNotExist()
        onNode(hasText("Second") and isHidden()).assertExists()
        onNode(hasText("First") and isHidden()).assertExists()
        holler.messages shouldHaveSize 3
    }

    @Test
    fun `for a host at the bottom the cards behind are smaller and show above the front one`() = runComposeUiTest {
        val holler = Holler()

        host(holler)
        show(holler, "First", "Second")

        val front = bounds("Second")
        val behind = bounds("First")

        behind.width shouldBe (front.width * ONE_STEP_BACK_SHARE plusOrMinus 1f)
        behind.height shouldBe (front.height * ONE_STEP_BACK_SHARE plusOrMinus 1f)
        withClue("the card behind must show above the front one") {
            behind.top shouldBeLessThan front.top
        }
        withClue("the card behind must not show below the front one") {
            behind.bottom shouldBeLessThanOrEqualTo front.bottom
        }
    }

    @Test
    fun `for a host at the top the cards behind show below the front one`() = runComposeUiTest {
        val holler = Holler()

        host(holler, alignment = Alignment.TopCenter)
        show(holler, "First", "Second")

        val front = bounds("Second")
        val behind = bounds("First")

        withClue("the card behind must show below the front one") {
            behind.bottom shouldBeGreaterThan front.bottom
        }
        withClue("the card behind must not show above the front one") {
            behind.top shouldBeGreaterThanOrEqualTo front.top
        }
    }

    @Test
    fun `a covered message keeps the time it had left and goes on when it is in front again`() = runComposeUiTest {
        val holler = Holler()

        host(holler)
        show(holler, "First")
        advanceTimeBy(1_200)

        onNodeWithText("First left:3").assertExists()

        // The second message stays its five seconds. The first one, covered, must not lose any of its three.
        show(holler, "Second")
        advanceTimeBy(4_200)

        onNodeWithText("Second").assertDoesNotExist()
        onNodeWithText("First left:3").assertExists()

        advanceTimeBy(3_500)

        holler.messages.shouldBeEmpty()
    }

    @Test
    fun `closing the front message brings the one behind it forward where it can be used`() = runComposeUiTest {
        val holler = Holler()

        host(holler)
        show(holler, "First", "Second")
        frontCloseButton().performClick()
        advanceTimeBy(PILE_SETTLE_MILLIS)

        onNode(hasText("First") and isHidden()).assertDoesNotExist()
        bounds("First").width shouldBe (CARD_FULL_WIDTH plusOrMinus 1f)

        frontCloseButton().performClick()
        advanceTimeBy(PILE_SETTLE_MILLIS)

        holler.messages.shouldBeEmpty()
    }

    @Test
    fun `messages beyond a full pile wait under it and are drawn when there is room`() = runComposeUiTest {
        val holler = Holler()

        host(holler, maxVisible = 2)
        show(holler, "First", "Second", "Third", "Fourth")

        withClue("a message two places beyond the pile must not be drawn") {
            isDrawn("First").shouldBeFalse()
        }
        withClue("the message right behind the pile must wait there") {
            isDrawn("Second").shouldBeTrue()
        }
        holler.messages shouldHaveSize 4

        frontCloseButton().performClick()
        advanceTimeBy(PILE_SETTLE_MILLIS)

        onNode(hasText("Third") and isHidden()).assertDoesNotExist()
        onNode(hasText("Second") and isHidden()).assertExists()
        withClue("the waiting message must be drawn once there is room") {
            isDrawn("First").shouldBeTrue()
        }
    }

    @Test
    fun `a message that comes back from under the pile goes on with the time it had left`() = runComposeUiTest {
        val holler = Holler()

        host(holler, maxVisible = 1)
        show(holler, "First")
        advanceTimeBy(1_200)

        onNodeWithText("First left:3").assertExists()

        show(holler, "Second", "Third")

        withClue("the first message must be too far under the pile to be drawn") {
            isDrawn("First").shouldBeFalse()
        }

        frontCloseButton().performClick()
        advanceTimeBy(PILE_SETTLE_MILLIS)
        frontCloseButton().performClick()
        advanceTimeBy(300)

        onNodeWithText("First left:3").assertExists()
    }

    @Test
    fun `a message that comes back from under the pile is still expanded and its timer still stopped`() = runComposeUiTest {
        val holler = Holler()

        host(holler, maxVisible = 1, withDetails = true)
        show(holler, "First")
        onNode(hasContentDescription(EXPAND_DESCRIPTION) and !isHidden()).performTouchInput { click() }
        advanceTimeBy(PILE_SETTLE_MILLIS)
        show(holler, "Second", "Third")

        withClue("the first message must be too far under the pile to be drawn") {
            isDrawn("First").shouldBeFalse()
        }

        frontCloseButton().performClick()
        advanceTimeBy(PILE_SETTLE_MILLIS)
        frontCloseButton().performClick()
        advanceTimeBy(PAST_SHORT_DURATION)

        onNodeWithText("First").assertExists()
        onNode(hasContentDescription(COLLAPSE_DESCRIPTION) and !isHidden()).assertExists()
    }

    @Test
    fun `the cards behind move to the height of a message that arrives in front`() = runComposeUiTest {
        val holler = Holler()

        host(holler)
        show(holler, "Short")

        val shortHeight = height("Short")

        holler.show(PileMessage(TALL))
        advanceTimeByFrame()
        advanceTimeBy(PILE_HALFWAY_MILLIS)

        val tallHeight = height(TALL)

        withClue("the card behind must be on its way to the height of the new front card") {
            height("Short") shouldBeGreaterThan shortHeight
            height("Short") shouldBeLessThan tallHeight
        }

        advanceTimeBy(PILE_SETTLE_MILLIS)

        height("Short") shouldBe tallHeight
    }

    @Test
    fun `a card that comes forward moves from the height it was held at to its own`() = runComposeUiTest {
        val holler = Holler()

        host(holler)
        show(holler, TALL)

        val tallHeight = height(TALL)

        show(holler, "Short")

        val shortHeight = height("Short")

        height(TALL) shouldBe shortHeight

        frontCloseButton().performClick()
        advanceTimeBy(PILE_HALFWAY_MILLIS)

        withClue("the card that came forward must be on its way to its own height") {
            height(TALL) shouldBeGreaterThan shortHeight
            height(TALL) shouldBeLessThan tallHeight
        }

        advanceTimeBy(PILE_SETTLE_MILLIS)

        height(TALL) shouldBe tallHeight
    }

    @Test
    fun `a card that comes forward without ever having been in front takes its own height at once`() = runComposeUiTest {
        val holler = Holler()

        host(holler)
        // Shown together, so the tall message is behind the short one from the start.
        holler.show(PileMessage(TALL))
        holler.show(PileMessage("Short"))
        advanceTimeBy(PILE_SETTLE_MILLIS)

        val shortHeight = height("Short")

        height(TALL) shouldBe shortHeight

        frontCloseButton().performClick()
        advanceTimeByFrame()
        advanceTimeByFrame()

        height(TALL) shouldBeGreaterThan shortHeight

        val ownHeight = height(TALL)

        advanceTimeBy(PILE_SETTLE_MILLIS)

        height(TALL) shouldBe ownHeight
    }

    @Test
    fun `the cards behind follow a front card that changes height while they are moving to it`() = runComposeUiTest {
        val holler = Holler()

        host(holler)
        show(holler, "Short")
        holler.show(PileMessage(TALL), key = "front")
        advanceTimeByFrame()
        advanceTimeBy(PILE_HALFWAY_MILLIS)

        // The front message gets a third line while the card behind is still moving to its height.
        holler.show(PileMessage(TALLER), key = "front")
        advanceTimeBy(PILE_HALFWAY_MILLIS)

        withClue("the card behind must not have jumped to the new height") {
            height("Short") shouldBeLessThan height(TALLER)
        }

        advanceTimeBy(PILE_SETTLE_MILLIS)

        height("Short") shouldBe height(TALLER)
    }

    @Test
    fun `closing two front cards one right after the other leaves the next one at its own height`() = runComposeUiTest {
        val holler = Holler()

        host(holler)
        show(holler, TALL)

        val tallHeight = height(TALL)

        show(holler, "Second", "Third")
        frontCloseButton().performClick()
        advanceTimeBy(PILE_HALFWAY_MILLIS)
        frontCloseButton().performClick()
        advanceTimeBy(PILE_HALFWAY_MILLIS)

        withClue("the tall card must be on its way to its own height") {
            height(TALL) shouldBeLessThan tallHeight
        }

        advanceTimeBy(PILE_SETTLE_MILLIS)

        height(TALL) shouldBe tallHeight
        bounds(TALL).width shouldBe (CARD_FULL_WIDTH plusOrMinus 1f)
    }

    @Test
    fun `for a host at the top a shorter card behind a taller one still shows below it`() = runComposeUiTest {
        val holler = Holler()

        host(holler, alignment = Alignment.TopCenter)
        show(holler, "Short", TALL)

        withClue("the shorter card must be as tall as the front one, so that it shows past it") {
            height("Short") shouldBe height(TALL)
        }
        withClue("the card behind must show below the front one") {
            bounds("Short").bottom shouldBeGreaterThan bounds(TALL).bottom
        }
    }

    @Test
    fun `a pile that was emptied starts again without moving any height`() = runComposeUiTest {
        val holler = Holler()

        host(holler)
        show(holler, TALL)

        val tallHeight = height(TALL)

        frontCloseButton().performClick()
        advanceTimeBy(PILE_SETTLE_MILLIS)

        holler.messages.shouldBeEmpty()

        holler.show(PileMessage("Short"))
        advanceTimeByFrame()
        advanceTimeByFrame()
        advanceTimeByFrame()

        val shortHeight = height("Short")

        shortHeight shouldBeLessThan tallHeight

        advanceTimeBy(PILE_SETTLE_MILLIS)

        height("Short") shouldBe shortHeight
    }

    @Test
    fun `a message closed from code while it waits under the pile is taken out of the queue`() = runComposeUiTest {
        val holler = Holler()

        host(holler, maxVisible = 1)
        holler.show(PileMessage("First"), key = "first")
        advanceTimeBy(PILE_SETTLE_MILLIS)
        show(holler, "Second", "Third")

        withClue("the first message must be too far under the pile to be drawn") {
            isDrawn("First").shouldBeFalse()
        }

        holler.dismiss("first")
        advanceTimeBy(PILE_SETTLE_MILLIS)

        holler.entries shouldHaveSize 2
        onNodeWithText("Third").assertExists()
    }

    @Test
    fun `replacing a covered message leaves it where it is`() = runComposeUiTest {
        val holler = Holler()

        host(holler)
        holler.show(PileMessage("First"), key = "first")
        advanceTimeBy(PILE_SETTLE_MILLIS)
        show(holler, "Second")
        holler.show(PileMessage("Replaced"), key = "first")
        advanceTimeBy(PILE_SETTLE_MILLIS)

        bounds("Second").width shouldBe (CARD_FULL_WIDTH plusOrMinus 1f)
        onNode(hasText("Replaced") and isHidden()).assertExists()
        holler.messages shouldHaveSize 2
    }

    @Test
    fun `pressing the front card presses it in and the card behind follows half as far`() = runComposeUiTest {
        val holler = Holler()

        host(holler)
        show(holler, "First", "Second")

        // The title is inside the front card, so it is drawn smaller with it.
        val frontTitleWidth = titleWidth("Second")
        val behindWidth = bounds("First").width

        onNodeWithText("Second").performTouchInput { down(center) }
        advanceTimeBy(PILE_SETTLE_MILLIS)

        titleWidth("Second") shouldBe (frontTitleWidth * PRESSED_FRONT_SHARE plusOrMinus 0.5f)
        bounds("First").width shouldBe (behindWidth * FOLLOWING_SHARE plusOrMinus 0.5f)

        onNodeWithText("Second").performTouchInput { up() }
        advanceTimeBy(PILE_SETTLE_MILLIS)

        bounds("First").width shouldBe (behindWidth plusOrMinus 0.5f)
    }

    @Test
    fun `a card fades in as it joins the back of the pile and out as it leaves it`() {
        pileCardAlpha(depth = 0f, size = 5) shouldBe 1f
        pileCardAlpha(depth = 4f, size = 5) shouldBe 1f
        pileCardAlpha(depth = 4.5f, size = 5) shouldBe 0.5f
        pileCardAlpha(depth = 5f, size = 5) shouldBe 0f
        pileCardAlpha(depth = 6f, size = 5) shouldBe 0f
    }

    @Test
    fun `a pile shows five messages by default and a column three`() {
        HollerDefaults.maxVisible(HollerStackingStyle.Pile) shouldBe 5
        HollerDefaults.maxVisible(HollerStackingStyle.Column) shouldBe 3
    }

    /**
     * Where a card is on screen right now, as it is drawn: smaller and moved when it is behind another.
     *
     * @param name the name of the card's message.
     * @return the bounds of the card.
     */
    private fun ComposeUiTest.bounds(name: String): Rect = runOnIdle { cards.getValue(name).boundsInRoot() }

    /**
     * Matches what is inside a card that is behind another one, which the pile hides from assistive
     * technology.
     *
     * @return the matcher.
     */
    private fun isHidden(): SemanticsMatcher {
        val hiddenCard = SemanticsMatcher.keyIsDefined(SemanticsProperties.HideFromAccessibility)

        return hasAnyAncestor(hiddenCard)
    }

    /**
     * The close button of the card in front. The cards behind it have one too, hidden with them.
     *
     * @return the close button that is not hidden.
     */
    private fun ComposeUiTest.frontCloseButton(): SemanticsNodeInteraction =
        onNode(hasContentDescription(CLOSE_DESCRIPTION) and !isHidden())

    /**
     * The width the title of a message in front is drawn at.
     *
     * @param name the name of the message.
     * @return the drawn width of its title, in pixels.
     */
    private fun ComposeUiTest.titleWidth(name: String): Float = onNodeWithText(name)
        .fetchSemanticsNode()
        .boundsInRoot
        .width

    /**
     * The height a card is laid out at, before the pile draws it smaller.
     *
     * @param name the name of the card's message.
     * @return the height of the card, in pixels.
     */
    private fun ComposeUiTest.height(name: String): Int = runOnIdle { cards.getValue(name).size.height }

    /**
     * Whether the card of a message is on screen right now.
     *
     * @param name the name of the card's message.
     * @return `true` when the card is part of what is drawn.
     */
    private fun ComposeUiTest.isDrawn(name: String): Boolean = runOnIdle { cards[name]?.isAttached == true }

    /**
     * Shows messages one after the other and lets the pile settle.
     *
     * @param holler the object to show them through.
     * @param names the names of the messages, in the order they are shown.
     */
    private fun ComposeUiTest.show(holler: Holler, vararg names: String) {
        names.forEach { name ->
            holler.show(PileMessage(name))
            advanceTimeByFrame()
        }

        advanceTimeBy(PILE_SETTLE_MILLIS)
    }

    /**
     * Puts a host that piles its messages on screen, with the test clock under manual control. Its
     * messages appear and leave at once, and a press does not pause them.
     *
     * @param holler the object whose messages are drawn.
     * @param alignment where the pile is placed.
     * @param maxVisible how many cards the pile holds.
     * @param withDetails whether every message has a long text, so it can be expanded.
     */
    private fun ComposeUiTest.host(
        holler: Holler,
        alignment: Alignment = Alignment.BottomCenter,
        maxVisible: Int = HollerDefaults.maxVisible(HollerStackingStyle.Pile),
        withDetails: Boolean = false,
    ) {
        mainClock.autoAdvance = false
        setContent {
            Box(Modifier.fillMaxSize()) {
                HollerHost(
                    holler = holler,
                    closeContentDescription = CLOSE_DESCRIPTION,
                    expandContentDescription = EXPAND_DESCRIPTION,
                    collapseContentDescription = COLLAPSE_DESCRIPTION,
                    alignment = alignment,
                    stackingStyle = HollerStackingStyle.Pile,
                    maxVisible = maxVisible,
                    animation = HollerAnimation.None,
                ) { value ->
                    val name = (value as PileMessage).name

                    HollerMessage(
                        title = name,
                        modifier = Modifier.onGloballyPositioned { cards[name] = it },
                        details = "$name details".takeIf { withDetails },
                        countdown = { Text("$name left:${it.inWholeSeconds}") },
                        pauseOnPressOrHover = false,
                    )
                }
            }
        }
    }

    private companion object {

        /** Width of a card at its full size in these tests, in pixels: the default maximum width. */
        const val CARD_FULL_WIDTH: Float = 375f
    }
}
