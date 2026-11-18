package io.github.mradzinski.holler

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.util.Collections
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.test.Test

/** How many threads each test starts at once. */
private const val THREADS: Int = 8

/** How many times each thread repeats its call. */
private const val CALLS_PER_THREAD: Int = 500

/** Tests that the host state keeps its queue consistent when several threads call it at once. */
class HollerThreadSafetyTest {

    @Test
    fun `no message is lost and no two share a number when threads show messages together`() {
        val state = Holler()

        onEveryThread { call -> state.show(call) }

        val expected = THREADS * CALLS_PER_THREAD
        state.messages shouldHaveSize expected
        state.entries.map { it.id }.toSet() shouldHaveSize expected
    }

    @Test
    fun `threads showing the same key end with one message however their calls interleave`() {
        val state = Holler()

        onEveryThread { call -> state.show(call, key = "shared") }

        state.messages shouldHaveSize 1
    }

    @Test
    fun `a message closed by several threads at once reports it only once`() {
        val state = Holler()
        val reasons = Collections.synchronizedList(mutableListOf<HollerDismissReason>())
        val handle = state.show(title = "Saved").reportTo(reasons)

        onEveryThread { handle.dismiss() }

        reasons.toList() shouldBe listOf(HollerDismissReason.Programmatic)
        state.messages shouldHaveSize 0
    }

    /**
     * Runs [block] on every thread at once, and waits for all of them to finish.
     *
     * @param block what each thread does on each of its calls; it receives a number no other call has.
     */
    private fun onEveryThread(block: (call: Int) -> Unit) {
        val start = CountDownLatch(1)

        val threads = List(THREADS) { index ->
            thread {
                start.await()
                repeat(CALLS_PER_THREAD) { call -> block(index * CALLS_PER_THREAD + call) }
            }
        }

        start.countDown()
        threads.forEach { it.join() }
    }
}
