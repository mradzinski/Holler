package io.github.mradzinski.holler

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Size of the placeholder drawn in the icon slot by the tests, in dp. */
private const val ICON_TEST_SIZE: Int = 10

/** Diameter of the ring icon under test, in dp (and pixels: the test density is 1). */
private const val ICON_TEST_RING_SIZE: Int = 50

/** Size the icon inside the ring asks for in the test, in dp: larger than the ring itself. */
private const val ICON_TEST_OVERSIZE: Int = 80

/** Height of a collapsed card with only a title, in dp (and pixels: the test density is 1). */
private const val COLLAPSED_CARD_HEIGHT: Int = 60

/** Size of the expand and close buttons in the header, in dp. */
private const val HEADER_BUTTON_SIZE: Int = 32

/** Size of the close button when it sits in the footer strip, in dp. */
private const val FOOTER_BUTTON_SIZE: Int = 28

/** A message long enough to wrap over several lines in the layout test. */
private const val LAYOUT_TEST_MESSAGE: String = "A message long enough to wrap over several lines, so that line heights count."

/** Width of the card in the tests that fix it, in dp (and pixels: the test density is 1). */
private const val SHADOW_TEST_CARD_WIDTH: Int = 300

/**
 * Tests of the forms of the API the main UI test does not go through: the slot and string-resource
 * overloads, per-message overrides, every ready-made animation, the status icons and the dark colors.
 */
@OptIn(ExperimentalTestApi::class)
class HollerOverloadsUiTest : UiTest() {

    /** The reasons reported by the message under test, in order. */
    private val reasons = mutableListOf<HollerDismissReason>()

    @Test
    fun `the slot form draws every slot it is given`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            TestCard(
                title = { Text("Slot title") },
                onDismiss = { reasons += it },
                closeContentDescription = CLOSE_DESCRIPTION,
                expandContentDescription = EXPAND_DESCRIPTION,
                collapseContentDescription = COLLAPSE_DESCRIPTION,
                details = { Text("Slot message") },
                icon = { Box(Modifier.size(ICON_TEST_SIZE.dp)) },
                actions = { TextButton(onClick = { dismiss() }) { Text("Go") } },
                countdown = { Text("left:${it.inWholeSeconds}") },
                actionsVisibility = HollerActionsVisibility.StartExpanded,
                // A message that starts expanded only keeps its timer, and so its countdown, when
                // nothing stops that timer.
                stoppableBy = HollerStopTrigger.None,
            )
        }

        advanceTimeBy(1_100)

        onNodeWithText("Slot title").assertExists()
        onNodeWithText("Slot message").assertExists()
        onNodeWithText("left:4").assertExists()

        onNodeWithText("Go").performClick()

        reasons shouldBe listOf(HollerDismissReason.Action)
    }

    @Test
    fun `a value is drawn with its own colors and style next to a text message with the host's`() = runComposeUiTest {
        val state = Holler()

        mainClock.autoAdvance = false
        setContent {
            val titleTextStyle = HollerDefaults
                .style().titleTextStyle
                .copy(color = Color.Red)

            val redTitleStyle = HollerDefaults.style(titleTextStyle = titleTextStyle)

            Box(Modifier.fillMaxSize()) {
                HollerHost(
                    holler = state,
                    closeContentDescription = CLOSE_DESCRIPTION,
                    expandContentDescription = EXPAND_DESCRIPTION,
                    collapseContentDescription = COLLAPSE_DESCRIPTION,
                    alignment = Alignment.TopCenter,
                ) {
                    HollerMessage(
                        title = { Text("Value in host") },
                        details = { Text("Value details") },
                        duration = HollerDuration.Infinite,
                        colors = HollerDefaults.colors(darkColorScheme()),
                        style = redTitleStyle,
                    )
                }
            }
        }

        waitForIdle()
        state.show(value = Unit)
        state.show(title = "Text in host", details = "Text details", duration = HollerDuration.Infinite)
        advanceTimeBy(1_000)

        onNodeWithText("Value in host").assertExists()
        onNodeWithText("Text in host").assertExists()

        state.dismissAll()
        advanceTimeBy(1_500)

        onNodeWithText("Value in host").assertDoesNotExist()
        state.entries.shouldBeEmpty()
    }

    @Test
    fun `each ready-made animation brings a message on screen and takes it off again`() {
        val animations = listOf(
            HollerAnimation.Default,
            HollerAnimation.Fade,
            HollerAnimation.Slide,
            HollerAnimation.Scale,
            HollerAnimation.Rise,
            HollerAnimation.Pop,
            HollerAnimation.Drop,
            HollerAnimation.None,
            HollerAnimation(
                messageEnter = fadeIn(),
                messageExit = fadeOut(),
                messageContentEnter = fadeIn(),
                messageContentExit = fadeOut(),
            ),
        )
        val placements = listOf(
            Alignment.BottomCenter to LayoutDirection.Ltr,
            Alignment.TopCenter to LayoutDirection.Rtl,
        )

        for (animation in animations) {
            for ((alignment, direction) in placements) {
                runComposeUiTest {
                    val state = Holler()

                    host(
                        state = state,
                        alignment = alignment,
                        layoutDirection = direction,
                        animation = animation,
                    )
                    val handle = state.show(title = "Animated")

                    advanceTimeBy(1_000)

                    onNodeWithText("Animated").assertExists()

                    handle.dismiss()
                    advanceTimeBy(1_500)

                    onNodeWithText("Animated").assertDoesNotExist()
                    state.entries.shouldBeEmpty()
                }
            }
        }
    }

    @Test
    fun `a message leaving a stack lets the others close the gap and the stack empties in order`() = runComposeUiTest {
        val state = Holler()

        host(state)
        val first = state.show(title = "First", duration = HollerDuration.Infinite)

        state.show(title = "Second", duration = HollerDuration.Infinite)
        advanceTimeBy(1_000)
        first.dismiss()
        advanceTimeBy(1_500)

        onNodeWithText("First").assertDoesNotExist()
        onNodeWithText("Second").assertExists()
        state.entries shouldHaveSize 1
    }

    @Test
    fun `the host refuses a visible-message limit below one`() = runComposeUiTest {
        shouldThrow<IllegalArgumentException> {
            setContent {
                HollerHost(
                    holler = Holler(),
                    closeContentDescription = CLOSE_DESCRIPTION,
                    expandContentDescription = EXPAND_DESCRIPTION,
                    collapseContentDescription = COLLAPSE_DESCRIPTION,
                    maxVisible = 0,
                )
            }

            waitForIdle()
        }
    }

    @Test
    fun `slots give Material text and icons their style and color and a button keeps its own`() = runComposeUiTest {
        lateinit var colors: HollerColors
        lateinit var style: HollerStyle
        lateinit var titleStyle: TextStyle
        var iconColor = Color.Unspecified
        var messageColor = Color.Unspecified
        var buttonLabelColor = Color.Unspecified
        var buttonColor = Color.Unspecified
        var primary = Color.Unspecified

        mainClock.autoAdvance = false
        setContent {
            colors = HollerDefaults.colors()
            style = HollerDefaults.style()
            primary = MaterialTheme.colorScheme.primary

            TestCard(
                title = {
                    titleStyle = LocalTextStyle.current
                    Text("Material title")
                },
                onDismiss = { reasons += it },
                closeContentDescription = CLOSE_DESCRIPTION,
                expandContentDescription = EXPAND_DESCRIPTION,
                collapseContentDescription = COLLAPSE_DESCRIPTION,
                details = {
                    messageColor = LocalContentColor.current
                    Text("Material message")
                },
                icon = { iconColor = LocalContentColor.current },
                actions = {
                    TextButton(onClick = { dismiss() }) {
                        buttonLabelColor = LocalTextStyle.current.color
                        buttonColor = LocalContentColor.current
                        Text("Go")
                    }
                },
                actionsVisibility = HollerActionsVisibility.StartExpanded,
            )
        }

        advanceTimeBy(1_000)

        onNodeWithText("Material title").assertExists()
        onNodeWithText("Material message").assertExists()
        titleStyle shouldBe style.titleTextStyle
        iconColor shouldBe colors.title
        messageColor shouldBe colors.details
        buttonLabelColor shouldBe Color.Unspecified
        buttonColor shouldBe primary
    }

    @Test
    fun `a text message needs no message lambda - the host draws it and it times out`() = runComposeUiTest {
        lateinit var state: Holler
        mainClock.autoAdvance = false
        setContent {
            state = remember { Holler() }
            Box(Modifier.fillMaxSize()) {
                HollerHost(
                    holler = state,
                    closeContentDescription = CLOSE_DESCRIPTION,
                    expandContentDescription = EXPAND_DESCRIPTION,
                    collapseContentDescription = COLLAPSE_DESCRIPTION,
                )
            }
        }

        state.show(title = "Text default")
        advanceTimeBy(1_000)

        onNodeWithText("Text default").assertExists()

        advanceTimeBy(6_000)

        state.entries.shouldBeEmpty()
    }

    @Test
    fun `an animation built from plain transitions leaves the content transitions off`() = runComposeUiTest {
        val state = Holler()

        host(
            state,
            animation = HollerAnimation(messageEnter = fadeIn(), messageExit = fadeOut()),
        )
        val handle = state.show(title = "Plain")

        advanceTimeBy(1_000)

        onNodeWithText("Plain").assertExists()

        handle.dismiss()
        advanceTimeBy(1_500)

        state.entries.shouldBeEmpty()
    }

    @Test
    fun `a message given as strings takes exactly the room of one drawn with foundation text`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            Column(Modifier.width(SHADOW_TEST_CARD_WIDTH.dp)) {
                TestCard(
                    title = "Same title",
                    onDismiss = { reasons += it },
                    closeContentDescription = CLOSE_DESCRIPTION,
                    expandContentDescription = EXPAND_DESCRIPTION,
                    collapseContentDescription = COLLAPSE_DESCRIPTION,
                    modifier = Modifier.testTag("strings"),
                    details = LAYOUT_TEST_MESSAGE,
                    actions = { TextButton(onClick = { dismiss() }) { Text("Go") } },
                    duration = HollerDuration.Infinite,
                    actionsVisibility = HollerActionsVisibility.StartExpanded,
                )

                TestCard(
                    title = { BasicText("Same title", style = LocalTextStyle.current) },
                    onDismiss = { reasons += it },
                    closeContentDescription = CLOSE_DESCRIPTION,
                    expandContentDescription = EXPAND_DESCRIPTION,
                    collapseContentDescription = COLLAPSE_DESCRIPTION,
                    modifier = Modifier.testTag("slots"),
                    details = { BasicText(LAYOUT_TEST_MESSAGE, style = LocalTextStyle.current) },
                    actions = { TextButton(onClick = { dismiss() }) { Text("Go") } },
                    duration = HollerDuration.Infinite,
                    actionsVisibility = HollerActionsVisibility.StartExpanded,
                )
            }
        }

        advanceTimeBy(1_000)

        val strings = onNodeWithTag("strings").fetchSemanticsNode().size
        val slots = onNodeWithTag("slots").fetchSemanticsNode().size

        strings.height shouldBeGreaterThan 0
        strings shouldBe slots
    }

    @Test
    fun `the card and its buttons keep their fixed sizes whatever Material's defaults are`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            Column(Modifier.width(SHADOW_TEST_CARD_WIDTH.dp)) {
                TestCard(
                    title = "Collapsed",
                    onDismiss = { reasons += it },
                    closeContentDescription = CLOSE_DESCRIPTION,
                    expandContentDescription = EXPAND_DESCRIPTION,
                    collapseContentDescription = COLLAPSE_DESCRIPTION,
                    modifier = Modifier.testTag("collapsed"),
                    duration = HollerDuration.Infinite,
                )

                TestCard(
                    title = "Expandable",
                    onDismiss = { reasons += it },
                    closeContentDescription = "Close the second",
                    expandContentDescription = EXPAND_DESCRIPTION,
                    collapseContentDescription = COLLAPSE_DESCRIPTION,
                    details = "Details",
                    showProgressBar = false,
                )

                TestCard(
                    title = "Closed from the bottom",
                    onDismiss = { reasons += it },
                    closeContentDescription = "Close at the bottom",
                    expandContentDescription = EXPAND_DESCRIPTION,
                    collapseContentDescription = COLLAPSE_DESCRIPTION,
                    closePosition = HollerClosePosition.Bottom,
                )
            }
        }

        advanceTimeBy(500)

        val card = onNodeWithTag("collapsed").fetchSemanticsNode().size
        val close = onNodeWithContentDescription(CLOSE_DESCRIPTION).fetchSemanticsNode().size
        val expand = onNodeWithContentDescription(EXPAND_DESCRIPTION).fetchSemanticsNode().size
        val footerClose = onNodeWithContentDescription("Close at the bottom").fetchSemanticsNode().size

        card shouldBe IntSize(SHADOW_TEST_CARD_WIDTH, COLLAPSED_CARD_HEIGHT)
        close shouldBe IntSize(HEADER_BUTTON_SIZE, HEADER_BUTTON_SIZE)
        expand shouldBe IntSize(HEADER_BUTTON_SIZE, HEADER_BUTTON_SIZE)
        footerClose shouldBe IntSize(FOOTER_BUTTON_SIZE, FOOTER_BUTTON_SIZE)
    }

    @Test
    fun `the icon ring tints the icon inside it and makes it fit`() = runComposeUiTest {
        var colorInside = Color.Unspecified
        var colorByDefault = Color.Unspecified

        setContent {
            Column {
                HollerIconRing(tint = Color.Green, size = ICON_TEST_RING_SIZE.dp) {
                    colorInside = LocalContentColor.current
                    Box(Modifier.size(ICON_TEST_OVERSIZE.dp).testTag("inside"))
                }

                CompositionLocalProvider(LocalContentColor provides Color.Cyan) {
                    HollerIconRing { colorByDefault = LocalContentColor.current }
                }
            }
        }

        val inside = onNodeWithTag("inside").fetchSemanticsNode().size

        colorInside shouldBe Color.Green
        colorByDefault shouldBe Color.Cyan
        withClue("the icon should fit inside the ring, was $inside") {
            inside.width shouldBeInRange (1 until ICON_TEST_RING_SIZE)
        }
        inside.height shouldBe inside.width
    }

    @Test
    fun `the default colors and text styles and shape follow the app's Material theme`() = runComposeUiTest {
        val scheme = darkColorScheme(
            primary = Color.Magenta,
            onSurface = Color.Yellow,
            surfaceContainerHigh = Color.Blue,
        )
        val typography = Typography().run {
            copy(titleSmall = titleSmall.copy(fontFamily = FontFamily.Monospace))
        }
        val shapes = Shapes(large = CutCornerShape(4.dp))
        lateinit var colors: HollerColors
        lateinit var style: HollerStyle

        setContent {
            MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes) {
                colors = HollerDefaults.colors()
                style = HollerDefaults.style()
            }
        }

        waitForIdle()

        colors shouldBe HollerDefaults.colors(scheme)
        colors.container shouldBe Color.Blue
        colors.title shouldBe Color.Yellow
        colors.progress shouldBe Color.Magenta
        style.titleTextStyle.fontFamily shouldBe FontFamily.Monospace
        style.shape shouldBe shapes.large
    }

    @Test
    fun `the shadow is stronger under a dark scheme`() {
        val light = HollerDefaults.colors(lightColorScheme())
        val dark = HollerDefaults.colors(darkColorScheme())

        dark.shadow.alpha shouldBeGreaterThan light.shadow.alpha
    }

    @Test
    fun `a short swipe is not enough - the card springs back and stays`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            TestCard(
                title = "Stay",
                onDismiss = { reasons += it },
                closeContentDescription = CLOSE_DESCRIPTION,
                expandContentDescription = EXPAND_DESCRIPTION,
                collapseContentDescription = COLLAPSE_DESCRIPTION,
                duration = HollerDuration.Infinite,
            )
        }

        advanceTimeByFrame()
        onNodeWithText("Stay").performTouchInput {
            swipeRight(
                startX = centerX,
                endX = centerX + 20f,
                durationMillis = 1_000,
            )
        }

        advanceTimeBy(2_000)

        onNodeWithText("Stay").assertExists()
        reasons.shouldBeEmpty()
    }

    @Test
    fun `the chevron hint plays and the progress bar draws in a right-to-left layout too`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TestCard(
                    title = "Hint",
                    onDismiss = { reasons += it },
                    closeContentDescription = CLOSE_DESCRIPTION,
                    expandContentDescription = EXPAND_DESCRIPTION,
                    collapseContentDescription = COLLAPSE_DESCRIPTION,
                    details = "Details",
                    actions = { TextButton(onClick = { dismiss() }) { Text("Okay") } },
                    actionsVisibility = HollerActionsVisibility.ChevronHint,
                )
            }
        }

        advanceTimeBy(2_500)

        onNodeWithText("Hint").assertExists()
        onNodeWithText("Okay").assertDoesNotExist()
        reasons.shouldBeEmpty()
    }

    /**
     * Puts a host on screen with the test clock under manual control.
     *
     * @param state the state whose messages are drawn.
     * @param alignment where messages are placed.
     * @param layoutDirection the reading direction of the host.
     * @param animation how messages enter and exit.
     * @param colors the colors of the messages.
     */
    private fun ComposeUiTest.host(
        state: Holler,
        alignment: Alignment = Alignment.BottomCenter,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        animation: HollerAnimation = HollerAnimation.Default,
        colors: HollerColors = HollerDefaults.colors(lightColorScheme()),
    ) {
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Box(Modifier.fillMaxSize()) {
                    HollerHost(
                        holler = state,
                        closeContentDescription = CLOSE_DESCRIPTION,
                        expandContentDescription = EXPAND_DESCRIPTION,
                        collapseContentDescription = COLLAPSE_DESCRIPTION,
                        alignment = alignment,
                        colors = colors,
                        animation = animation,
                    )
                }
            }
        }
    }
}
