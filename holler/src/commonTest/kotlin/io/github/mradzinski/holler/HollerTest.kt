package io.github.mradzinski.holler

import io.github.mradzinski.holler.internal.TextMessage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

/** The values the tests show, the way an app would define its own messages. */
private enum class TestValue {
    /** A message that is later replaced. */
    Sending,

    /** The message that replaces [Sending]. */
    Sent,

    /** A message with no relation to the others. */
    Other,
}

/** Tests of the bookkeeping of [Holler], without drawing anything. */
@OptIn(ExperimentalCoroutinesApi::class)
class HollerTest {

    @Test
    fun `messages are kept in the order they were shown`() {
        val state = Holler()

        state.show(title = "First")
        state.show(title = "Second")

        state.entries.map { it.id } shouldBe listOf(0L, 1L)
    }

    @Test
    fun `closing a message that never became visible removes it at once and reports it once`() {
        val state = Holler()
        val reasons = mutableListOf<HollerDismissReason>()
        val handle = state.show(title = "Queued").reportTo(reasons)

        handle.dismiss()
        handle.dismiss()

        state.entries.shouldBeEmpty()
        reasons shouldBe listOf(HollerDismissReason.Programmatic)
    }

    @Test
    fun `closing everything reports every message and empties the queue`() {
        val state = Holler()
        val reasons = mutableListOf<HollerDismissReason>()

        repeat(4) { state.show(title = "Message $it").reportTo(reasons) }

        state.dismissAll()

        reasons shouldBe List(4) { HollerDismissReason.Programmatic }
        state.entries.shouldBeEmpty()
    }

    @Test
    fun `a message shown with the key of a waiting one takes its place in the queue and its handle`() {
        val state = Holler()
        val reasons = mutableListOf<HollerDismissReason>()

        state.show(title = "First")
        val original = state.show(title = "Uploading", key = "upload").reportTo(reasons)
        state.show(title = "Last")

        val replacement = state.show(title = "Uploaded", duration = HollerDuration.Long, key = "upload")

        replacement shouldBeSameInstanceAs original
        replacement.key shouldBe "upload"
        state.entries.map { it.id } shouldBe listOf(0L, 1L, 2L)
        state.entries[1].content.value shouldBe TextMessage("Uploaded", details = null, HollerDuration.Long)
        reasons shouldBe listOf(HollerDismissReason.Replaced)
    }

    @Test
    fun `without a key every message gets its own and that key replaces it like any other`() {
        val state = Holler()

        val first = state.show(title = "First")
        val second = state.show(title = "Second")

        second.key shouldNotBe first.key
        state.entries shouldHaveSize 2

        val replacement = state.show(title = "First again", key = first.key)

        replacement shouldBeSameInstanceAs first
        state.entries shouldHaveSize 2
    }

    @Test
    fun `closing by key closes the message that has it and does nothing for a key nobody has`() {
        val state = Holler()
        val reasons = mutableListOf<HollerDismissReason>()

        state.show(title = "Stays", key = "stays")
        state.show(title = "Goes", key = "goes").reportTo(reasons)

        state.dismiss("unknown")

        state.entries shouldHaveSize 2

        state.dismiss("goes")

        state.entries.map { it.key } shouldBe listOf("stays")
        reasons shouldBe listOf(HollerDismissReason.Programmatic)
    }

    @Test
    fun `a message that is already closing is not replaced - its key shows a new message`() {
        val state = Holler()
        val reasons = mutableListOf<HollerDismissReason>()
        val closing = state.show(title = "Old", key = "status").reportTo(reasons)

        state.entries
            .single()
            .markShown()
        closing.dismiss()

        val shown = state.show(title = "New", key = "status")
        state.dismiss("status")

        shown shouldNotBe closing
        state.entries.map { it.id } shouldBe listOf(0L)
        reasons shouldBe listOf(HollerDismissReason.Programmatic)
    }

    @Test
    fun `a value waits in the queue as it was shown where a test can read it`() {
        val state = Holler()

        val handle = state.show(TestValue.Sending)
        state.show(title = "Plain text")

        handle.value shouldBe TestValue.Sending
        state.messages.map { it.value } shouldBe listOf(TestValue.Sending, null)
    }

    @Test
    fun `showing a value with the key of another replaces it and keeps its handle and place`() {
        val state = Holler()

        val first = state.show(TestValue.Sending, key = "reply")
        state.show(TestValue.Other)
        val second = state.show(TestValue.Sent, key = "reply")

        second shouldBeSameInstanceAs first
        state.messages.map { it.value } shouldBe listOf(TestValue.Sent, TestValue.Other)
    }

    @Test
    fun `a closed message is no longer among the messages and its key is free again`() {
        val state = Holler()

        state.show(TestValue.Other, key = "offline")
        state.dismiss("offline")

        state.messages.shouldBeEmpty()
    }

    @Test
    fun `a message shown as text has no value`() {
        val state = Holler()

        val handle = state.show(title = "Saved", details = "Everything is up to date.")

        handle.value.shouldBeNull()
    }

    @Test
    fun `a wait before a replace learns of it and one after it waits for what replaced it`() {
        val state = Holler()
        val first = mutableListOf<HollerDismissReason>()
        val second = mutableListOf<HollerDismissReason>()

        state.show(TestValue.Other, key = "status").reportTo(first)
        state.show(TestValue.Sent, key = "status").reportTo(second)
        state.dismiss("status")

        first shouldBe listOf(HollerDismissReason.Replaced)
        second shouldBe listOf(HollerDismissReason.Programmatic)
        state.entries.shouldBeEmpty()
    }

    @Test
    fun `waiting for a message that has already closed returns its reason at once`() = runTest {
        val state = Holler()
        val handle = state.show(title = "Gone")

        handle.dismiss()

        handle.awaitDismiss() shouldBe HollerDismissReason.Programmatic
    }

    @Test
    fun `every caller waiting for one message gets the reason`() {
        val state = Holler()
        val reasons = mutableListOf<HollerDismissReason>()
        val handle = state.show(title = "Shared")

        handle.reportTo(reasons)
        handle.reportTo(reasons)
        handle.dismiss()

        reasons shouldBe List(2) { HollerDismissReason.Programmatic }
    }

    @Test
    fun `a caller that stops waiting is forgotten at once and its message stays`() = runTest {
        val state = Holler()
        val handle = state.show(title = "Stays")
        val waiting = launch(UnconfinedTestDispatcher(testScheduler)) { handle.awaitDismiss() }

        state.entries.single().content.waiters shouldHaveSize 1

        waiting.cancel()

        state.entries.single().content.waiters.shouldBeEmpty()
        state.messages shouldHaveSize 1
    }

    @Test
    fun `a duration passed where the key goes is refused`() {
        val state = Holler()

        shouldThrow<IllegalArgumentException> { state.show("Saved", HollerDuration.Long) }
        state.entries.shouldBeEmpty()
    }
}
