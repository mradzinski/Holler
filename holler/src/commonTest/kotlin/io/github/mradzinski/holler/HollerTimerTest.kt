package io.github.mradzinski.holler

import io.github.mradzinski.holler.internal.HollerTimer
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource

/** Tests of the timer in its delay-driven mode (the frame-driven mode is covered by the UI tests). */
@OptIn(ExperimentalCoroutinesApi::class)
class HollerTimerTest {

    @Test
    fun `the timer finishes exactly when the full time has passed`() = runTest {
        val timer = HollerTimer(3.seconds)

        timer.run(frameDriven = false, tickWholeSeconds = true)

        timer.isFinished.shouldBeTrue()
        currentTime shouldBe 3000L
        timer.remaining.value shouldBe Duration.ZERO
    }

    @Test
    fun `without a countdown the timer waits once for the whole time and still finishes on time`() = runTest {
        val timer = HollerTimer(2500.milliseconds)

        timer.run(frameDriven = false, tickWholeSeconds = false)

        timer.isFinished.shouldBeTrue()
        currentTime shouldBe 2500L
    }

    @Test
    fun `the remaining time is rounded up to whole seconds and drops once per second`() = runTest {
        val timer = HollerTimer(2500.milliseconds)

        timer.remaining.value shouldBe 3.seconds

        val job = launch { timer.run(frameDriven = false, tickWholeSeconds = true) }

        advanceTimeBy(1001)

        timer.remaining.value shouldBe 2.seconds

        advanceTimeBy(1000)

        timer.remaining.value shouldBe 1.seconds

        job.join()

        timer.isFinished.shouldBeTrue()
    }

    @Test
    fun `a stopped timer never finishes and keeps the time it had left`() = runTest {
        val timer = HollerTimer(3.seconds)

        launch { timer.run(frameDriven = false, tickWholeSeconds = true) }
        advanceTimeBy(1500)
        timer.stop()
        advanceUntilIdle()

        timer.isFinished.shouldBeFalse()
        timer.isActive.shouldBeFalse()
        timer.remaining.value shouldBe 2.seconds
    }

    @Test
    fun `a paused timer keeps its time and finishes on schedule once resumed`() = runTest {
        val timer = HollerTimer(3.seconds, testTimeSource)
        val counting = launch { timer.run(frameDriven = false, tickWholeSeconds = true) }

        advanceTimeBy(1000)
        timer.pause()
        // The card restarts its counting when the timer pauses, which cancels the wait in progress.
        counting.cancelAndJoin()

        timer.isPaused.shouldBeTrue()
        timer.isActive.shouldBeTrue()

        advanceTimeBy(60_000)

        timer.isFinished.shouldBeFalse()
        timer.remaining.value shouldBe 2.seconds

        timer.resume()

        timer.isPaused.shouldBeFalse()

        val resumedAt = currentTime

        timer.run(frameDriven = false, tickWholeSeconds = true)

        timer.isFinished.shouldBeTrue()
        currentTime - resumedAt shouldBe 2000L
    }

    @Test
    fun `a wait cut short by a pause is credited with the time that really passed`() = runTest {
        val timer = HollerTimer(4.seconds, testTimeSource)
        val counting = launch { timer.run(frameDriven = false, tickWholeSeconds = false) }

        advanceTimeBy(1500)
        timer.pause()
        counting.cancelAndJoin()

        timer.remaining.value shouldBe 3.seconds

        timer.resume()
        val resumedAt = currentTime

        timer.run(frameDriven = false, tickWholeSeconds = false)

        timer.isFinished.shouldBeTrue()
        currentTime - resumedAt shouldBe 2500L
    }

    @Test
    fun `a timer paused at the very end is held short of finishing and finishes when resumed`() = runTest {
        val timer = HollerTimer(1.seconds, testTimeSource)
        val counting = launch { timer.run(frameDriven = false, tickWholeSeconds = false) }

        advanceTimeBy(999)
        timer.pause()
        advanceTimeBy(5_000)
        counting.cancelAndJoin()

        timer.isFinished.shouldBeFalse()

        timer.resume()
        timer.run(frameDriven = false, tickWholeSeconds = false)

        timer.isFinished.shouldBeTrue()
    }

    @Test
    fun `pausing and resuming do nothing to a stopped timer and a paused timer can be stopped`() = runTest {
        val stopped = HollerTimer(3.seconds, testTimeSource)

        stopped.stop()
        stopped.pause()
        stopped.resume()

        stopped.isStopped.shouldBeTrue()
        stopped.isPaused.shouldBeFalse()

        val paused = HollerTimer(3.seconds, testTimeSource)

        paused.pause()
        paused.stop()

        paused.isStopped.shouldBeTrue()
        paused.isActive.shouldBeFalse()
    }

    @Test
    fun `an infinite timer has nothing to show and returns at once without ever finishing`() = runTest {
        val timer = HollerTimer(Duration.INFINITE)

        timer.run(frameDriven = false, tickWholeSeconds = true)

        timer.isFinite.shouldBeFalse()
        timer.isActive.shouldBeFalse()
        timer.isFinished.shouldBeFalse()
        currentTime shouldBe 0L
    }

    @Test
    fun `an infinite timer shows no progress`() {
        val timer = HollerTimer(Duration.INFINITE)

        timer.progress shouldBe 0f
    }
}
