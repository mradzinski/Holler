package io.github.mradzinski.holler

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.property.Arb
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

/** How many generated sequences of steps each run of the test checks. */
private const val SEQUENCES: Int = 200

/** The longest sequence of steps the test generates. */
private const val MAX_STEPS: Int = 40

/** The highest position a generated step points at, in the handles or in the queue. */
private const val MAX_INDEX: Int = 7

/** The keys the generated steps show and close. They are few, so the steps often meet on a key. */
private val Keys: List<String> = listOf("a", "b", "c")

/** One thing that can happen to a [Holler]: a call the app makes, or a step its host takes. */
private sealed interface Step {

    /**
     * The app shows a value.
     *
     * @property key the key it is shown with, or `null` for a message with a key of its own.
     */
    data class Show(val key: String?) : Step

    /**
     * The app closes a message by its key.
     *
     * @property key the key to close, which may belong to no message.
     */
    data class DismissKey(val key: String) : Step

    /**
     * The app closes a message through a handle it got earlier.
     *
     * @property index which of the handles returned so far, oldest first; past the last one, nothing happens.
     */
    data class DismissHandle(val index: Int) : Step

    /** The app closes every message. */
    data object DismissAll : Step

    /**
     * The host starts drawing a message of the queue.
     *
     * @property index the place of the message in the queue; past the last one, nothing happens.
     */
    data class Draw(val index: Int) : Step

    /**
     * The host forgets a closing message, as it does when the exit animation has ended.
     *
     * @property index which of the closing messages, oldest first; past the last one, nothing happens.
     */
    data class Forget(val index: Int) : Step
}

/** One call to `show`, with what its caller was told and what it should have been told. */
private class Call {

    /** The reasons the caller waiting on this call received. A correct queue adds at most one. */
    val reasons: MutableList<HollerDismissReason> = mutableListOf()

    /** The reason the caller must have received by now, or `null` while its message is still alive. */
    var expected: HollerDismissReason? = null
}

/**
 * A message the test expects to be alive: on screen or waiting.
 *
 * @param handle the handle `show` returned for it.
 * @param call the call whose content the message shows now.
 */
private class Live(val handle: HollerHandle, var call: Call)

/**
 * A [Holler] next to a simple description of what it should hold. Each step is applied to both, and
 * [check] compares them.
 */
private class Scenario {

    /** The queue under test. */
    private val holler = Holler()

    /** Every call to `show` so far, oldest first. */
    private val calls = mutableListOf<Call>()

    /** Every handle `show` returned so far, each once, oldest first. */
    private val handles = mutableListOf<HollerHandle>()

    /** The messages that should be alive, oldest first. */
    private val live = mutableListOf<Live>()

    /** The messages the host has started drawing. Only these may still be in the queue while closing. */
    private val drawn = mutableListOf<HollerHandle>()

    /**
     * Applies a step to the queue and to the description of what it should hold.
     *
     * @param step the step to take.
     */
    fun run(step: Step) {
        when (step) {
            is Step.Show -> show(step.key)

            is Step.DismissKey -> {
                holler.dismiss(step.key)
                close(live.firstOrNull { it.handle.key == step.key })
            }

            is Step.DismissHandle -> handles.getOrNull(step.index)?.let { handle ->
                handle.dismiss()
                close(live.firstOrNull { it.handle === handle })
            }

            is Step.DismissAll -> {
                holler.dismissAll()
                live.toList().forEach { close(it) }
            }

            is Step.Draw -> holler.entries.getOrNull(step.index)?.let { entry ->
                if (!entry.isDismissed) drawn += entry

                entry.markShown()
            }

            is Step.Forget -> {
                val closing = holler.entries.filter { it.isDismissed }

                closing.getOrNull(step.index)?.let { holler.remove(it) }
            }
        }
    }

    /** Compares the queue with what it should hold after the steps taken so far. */
    fun check() {
        val ids = holler.entries.map { it.id }
        val closing = holler.entries.filter { it.isDismissed }

        // The alive messages, in the order they were first shown, and nothing else.
        holler.messages shouldContainExactly live.map { it.handle }

        // No two alive messages share a key.
        live.map { it.handle.key }.distinct() shouldHaveSize live.size

        // Every caller was told how its message ended exactly when it ended, and only once.
        calls.forEach { it.reasons shouldBe listOfNotNull(it.expected) }

        // The numbers of the messages are unique and follow the order of the queue.
        ids shouldBe ids.sorted().distinct()

        // A message that closes before it was drawn has no exit to play, so it leaves the queue at once.
        drawn shouldContainAll closing
    }

    /** Checks that nothing is alive and that every caller was told how its message ended. */
    fun checkEverythingClosed() {
        holler.messages.shouldBeEmpty()
        calls.forEach { it.reasons shouldHaveSize 1 }
    }

    /**
     * Shows a value and waits on it, as an app would. A key that an alive message has replaces that
     * message: its handle is returned again and the caller that was waiting on it is told so.
     *
     * @param key the key to show the value with, or `null` for a message with a key of its own.
     */
    private fun show(key: String?) {
        val call = Call().also { calls += it }
        val handle = holler
            .show(value = calls.size, key = key)
            .reportTo(call.reasons)
        val replaced = key?.let { live.firstOrNull { message -> message.handle.key == it } }

        if (replaced != null) {
            handle shouldBeSameInstanceAs replaced.handle
            replaced.call.expected = HollerDismissReason.Replaced
            replaced.call = call
        } else {
            handles += handle
            live += Live(handle, call)
        }
    }

    /**
     * Records that an alive message was closed from code.
     *
     * @param message the message, or `null` when the step met no alive message and nothing changes.
     */
    private fun close(message: Live?) {
        if (message == null) return

        message.call.expected = HollerDismissReason.Programmatic
        live -= message
    }
}

/**
 * Tests of [Holler] against sequences of steps generated at random: whatever the app and the host do,
 * and in whatever order, the queue and the callers waiting on it must stay consistent. A failure
 * prints the shortest sequence that still fails and the seed that repeats it.
 */
class HollerPropertyTest {

    @Test
    fun `any sequence of steps keeps the queue and its waiting callers consistent`() = runTest {
        checkAll(SEQUENCES, Arb.list(steps(), 0..MAX_STEPS)) { steps ->
            val scenario = Scenario()

            steps.forEach { step ->
                scenario.run(step)
                scenario.check()
            }

            scenario.run(Step.DismissAll)

            scenario.check()
            scenario.checkEverythingClosed()
        }
    }

    /**
     * Generates one step. Showing is listed twice, so a sequence builds up a queue for the other
     * steps to act on.
     *
     * @return the generator of steps.
     */
    private fun steps(): Arb<Step> = Arb.choice(
        Arb.element(Keys + null).map { Step.Show(it) },
        Arb.element(Keys + null).map { Step.Show(it) },
        Arb.element(Keys).map { Step.DismissKey(it) },
        Arb.int(0..MAX_INDEX).map { Step.DismissHandle(it) },
        Arb.constant(Step.DismissAll),
        Arb.int(0..MAX_INDEX).map { Step.Draw(it) },
        Arb.int(0..MAX_INDEX).map { Step.Forget(it) },
    )
}
