package io.github.mradzinski.holler

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

/** The messages the tests show as values, the way an app would define its own. */
private sealed interface TestMessage {

    /**
     * A message with an Undo action.
     *
     * @property id what the action is about.
     */
    data class Deleted(val id: Int) : TestMessage

    /** A message drawn with plain text. */
    data object Saved : TestMessage

    /** A message the host draws nothing for. */
    data object Unknown : TestMessage
}

/** How long the slow test animation takes each way, in milliseconds: long enough to catch it half way. */
private const val SLOW_FADE_MILLIS: Int = 1_000

/** An animation slow enough for a test to tell it apart from no animation. */
private val SlowFade: HollerAnimation = HollerAnimation(
    messageEnter = fadeIn(tween(SLOW_FADE_MILLIS)),
    messageExit = fadeOut(tween(SLOW_FADE_MILLIS)),
)

/** The key the tests give the message they replace. */
private const val VALUE_KEY: String = "reply"

/** Tests of messages shown as values, which the host's content gives a look. */
@OptIn(ExperimentalTestApi::class)
class HollerValueUiTest : UiTest() {

    /** The reasons reported by the messages under test, in order. */
    private val reasons = mutableListOf<HollerDismissReason>()

    /** The ids the Undo action was pressed for, in order. */
    private val undone = mutableListOf<Int>()

    @Test
    fun `the host's message lambda decides what a value looks like`() = runComposeUiTest {
        val state = Holler()

        host(state)
        state.show(TestMessage.Saved)
        advanceTimeBy(1_000)

        onNodeWithText("Saved").assertExists()
        onNodeWithText("Saved details").assertDoesNotExist()
        onNodeWithContentDescription(EXPAND_DESCRIPTION).assertExists()
    }

    @Test
    fun `a message's own animation is played when it enters and exits in place of the host's`() = runComposeUiTest {
        val state = Holler()

        // The host has no animation at all, so anything still moving comes from the message's own.
        animatedHost(state, hostAnimation = HollerAnimation.None, messageAnimation = SlowFade)

        val handle = state.show(value = Unit)
        advanceTimeBy(SLOW_FADE_MILLIS / 2L)

        val visibility = state.entries.single().visibility

        withClue("the message should still be entering") {
            visibility.isIdle.shouldBeFalse()
        }

        advanceTimeBy(SLOW_FADE_MILLIS.toLong())

        visibility.shouldHaveEntered()

        handle.dismiss()
        advanceTimeBy(SLOW_FADE_MILLIS / 2L)

        withClue("the message should still be leaving") {
            state.entries shouldHaveSize 1
        }

        advanceTimeBy(SLOW_FADE_MILLIS.toLong())

        state.entries.shouldBeEmpty()
    }

    @Test
    fun `a message that asks for no animation of its own gets the host's`() = runComposeUiTest {
        val state = Holler()

        animatedHost(state, hostAnimation = SlowFade, messageAnimation = null)

        state.show(value = Unit)
        advanceTimeBy(SLOW_FADE_MILLIS / 2L)

        val visibility = state.entries.single().visibility

        withClue("the message should still be entering") {
            visibility.isIdle.shouldBeFalse()
        }

        advanceTimeBy(SLOW_FADE_MILLIS.toLong())

        visibility.shouldHaveEntered()
    }

    @Test
    fun `a message replaced while it is still entering goes on entering`() = runComposeUiTest {
        val state = Holler()

        animatedHost(state, hostAnimation = SlowFade, messageAnimation = null)

        state.show(value = 1, key = VALUE_KEY)
        advanceTimeBy(SLOW_FADE_MILLIS / 2L)

        state.show(value = 2, key = VALUE_KEY)
        advanceTimeBy(SLOW_FADE_MILLIS / 2L + 100L)

        val visibility = state.entries.single().visibility

        withClue("the enter animation should have ended on time") {
            visibility.shouldHaveEntered()
        }
    }

    @Test
    fun `with no animation on the host or on the message the message is simply there`() = runComposeUiTest {
        val state = Holler()

        animatedHost(state, hostAnimation = HollerAnimation.None, messageAnimation = null)

        state.show(value = Unit)
        advanceTimeBy(SLOW_FADE_MILLIS / 2L)

        val visibility = state.entries.single().visibility

        visibility.shouldHaveEntered()
    }

    @Test
    fun `a message that reads state follows that state and its timer is not started over`() = runComposeUiTest {
        val state = Holler()
        var percent by mutableStateOf(0)

        mainClock.autoAdvance = false
        setContent {
            Box(Modifier.fillMaxSize()) {
                HollerHost(
                    holler = state,
                    closeContentDescription = CLOSE_DESCRIPTION,
                    expandContentDescription = EXPAND_DESCRIPTION,
                    collapseContentDescription = COLLAPSE_DESCRIPTION,
                ) {
                    HollerMessage(title = "Uploading $percent%", duration = HollerDuration.Custom(4.seconds))
                }
            }
        }

        state.show(value = Unit)
        advanceTimeBy(2_000)

        percent = 40
        advanceTimeByFrame()

        onNodeWithText("Uploading 40%").assertExists()

        // Four seconds after it appeared, not four seconds after the change.
        advanceTimeBy(3_000)

        onNodeWithText("Uploading 40%").assertDoesNotExist()
        state.messages.shouldBeEmpty()
    }

    @Test
    fun `an action reads the value and can close the message which reports the reason`() = runComposeUiTest {
        val state = Holler()

        host(state)
        state.show(TestMessage.Deleted(id = 7)).reportTo(reasons)
        advanceTimeBy(1_000)

        onNodeWithText("Undo").performClick()
        advanceTimeBy(2_000)

        undone shouldBe listOf(7)
        reasons shouldBe listOf(HollerDismissReason.Action)
        onNodeWithText("Deleted").assertDoesNotExist()
        state.messages.shouldBeEmpty()
    }

    @Test
    fun `a value message runs the timer of the duration its look gives it`() = runComposeUiTest {
        val state = Holler()

        host(state)
        state.show(TestMessage.Deleted(id = 1)).reportTo(reasons)
        advanceTimeBy(2_000)

        onNodeWithText("Deleted").assertExists()

        advanceTimeBy(3_000)

        onNodeWithText("Deleted").assertDoesNotExist()
        reasons shouldBe listOf(HollerDismissReason.Timeout)
        state.entries.shouldBeEmpty()
    }

    @Test
    fun `a message drawn with slots and no duration closes after the short one`() = runComposeUiTest {
        val state = Holler()

        mainClock.autoAdvance = false
        setContent {
            Box(Modifier.fillMaxSize()) {
                HollerHost(
                    holler = state,
                    closeContentDescription = CLOSE_DESCRIPTION,
                    expandContentDescription = EXPAND_DESCRIPTION,
                    collapseContentDescription = COLLAPSE_DESCRIPTION,
                ) {
                    HollerMessage(title = { Text("Slots") })
                }
            }
        }

        state.show(TestMessage.Unknown).reportTo(reasons)
        advanceTimeBy(4_000)

        onNodeWithText("Slots").assertExists()
        reasons.shouldBeEmpty()

        advanceTimeBy(2_000)

        onNodeWithText("Slots").assertDoesNotExist()
        reasons shouldBe listOf(HollerDismissReason.Timeout)
    }

    @Test
    fun `showing another value with the same key swaps the look in the same card`() = runComposeUiTest {
        val state = Holler()

        host(state)
        val handle = state.show(TestMessage.Deleted(id = 1), key = VALUE_KEY).reportTo(reasons)
        advanceTimeBy(1_000)

        val replaced = state.show(TestMessage.Saved, key = VALUE_KEY)
        advanceTimeByFrame()

        val visibility = state.entries.single().visibility

        onNodeWithText("Deleted").assertDoesNotExist()
        onNodeWithText("Saved").assertExists()
        visibility.shouldHaveEntered()
        handle shouldBeSameInstanceAs replaced
        handle.value shouldBe TestMessage.Saved
        reasons shouldBe listOf(HollerDismissReason.Replaced)
    }

    @Test
    fun `a value the host draws nothing for does not stay in the queue`() = runComposeUiTest {
        val state = Holler()

        host(state)
        state.show(TestMessage.Unknown)
        state.show(TestMessage.Saved)
        advanceTimeBy(1_000)

        state.messages.map { it.value } shouldBe listOf(TestMessage.Saved)
        onNodeWithText("Saved").assertExists()
    }

    @Test
    fun `a host with no message lambda drops a value shown through it`() = runComposeUiTest {
        val state = Holler()

        setContent {
            HollerHost(
                holler = state,
                closeContentDescription = CLOSE_DESCRIPTION,
                expandContentDescription = EXPAND_DESCRIPTION,
                collapseContentDescription = COLLAPSE_DESCRIPTION,
            )
        }

        state.show(TestMessage.Saved)
        advanceTimeBy(1_000)

        state.entries.shouldBeEmpty()
    }

    @Test
    fun `closing a value message through its handle reports it`() = runComposeUiTest {
        val state = Holler()

        host(state)
        val handle = state.show(TestMessage.Deleted(id = 1), key = VALUE_KEY).reportTo(reasons)
        advanceTimeBy(1_000)

        handle.dismiss()
        advanceTimeBy(2_000)

        reasons shouldBe listOf(HollerDismissReason.Programmatic)
        state.entries.shouldBeEmpty()
    }

    @Test
    fun `the host's scope fails when it is used outside its host`() = runComposeUiTest {
        val state = Holler()
        var captured: HollerHostScope? = null

        setContent {
            HollerHost(
                holler = state,
                closeContentDescription = CLOSE_DESCRIPTION,
                expandContentDescription = EXPAND_DESCRIPTION,
                collapseContentDescription = COLLAPSE_DESCRIPTION,
            ) {
                captured = this

                HollerMessage(title = "Saved", duration = HollerDuration.Infinite)
            }
        }

        state.show(TestMessage.Saved)
        advanceTimeBy(1_000)

        val scope = checkNotNull(captured)

        shouldThrow<IllegalStateException> {
            setContent { scope.HollerMessage(title = "Saved") }
        }
    }

    /**
     * Shows a host that gives every [TestMessage] but [TestMessage.Unknown] a look.
     *
     * @param state the state the host draws.
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
                ) { value ->
                    when (val message = value) {
                        is TestMessage.Deleted -> HollerMessage(
                            title = { Text("Deleted") },
                            actions = {
                                TextButton(
                                    onClick = {
                                        undone += message.id
                                        dismiss()
                                    },
                                ) {
                                    Text("Undo")
                                }
                            },
                            duration = HollerDuration.Custom(4.seconds),
                        )

                        is TestMessage.Saved -> HollerMessage(
                            title = "Saved",
                            details = "Saved details",
                            duration = HollerDuration.Infinite,
                        )

                        else -> Unit
                    }
                }
            }
        }
    }

    /**
     * Puts a host on screen that draws every value as one message, with the test clock under manual
     * control.
     *
     * @param state the object whose messages are drawn.
     * @param hostAnimation the animation of the host.
     * @param messageAnimation the animation the message asks for, or `null` for none of its own.
     */
    private fun ComposeUiTest.animatedHost(
        state: Holler,
        hostAnimation: HollerAnimation,
        messageAnimation: HollerAnimation?,
    ) {
        mainClock.autoAdvance = false
        setContent {
            Box(Modifier.fillMaxSize()) {
                HollerHost(
                    holler = state,
                    closeContentDescription = CLOSE_DESCRIPTION,
                    expandContentDescription = EXPAND_DESCRIPTION,
                    collapseContentDescription = COLLAPSE_DESCRIPTION,
                    animation = hostAnimation,
                ) {
                    HollerMessage(
                        title = "Animated",
                        duration = HollerDuration.Infinite,
                        animation = messageAnimation,
                    )
                }
            }
        }
    }
}
