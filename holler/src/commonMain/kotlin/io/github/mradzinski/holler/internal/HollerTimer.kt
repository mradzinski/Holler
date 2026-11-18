package io.github.mradzinski.holler.internal

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.delay

/**
 * The single source of truth for a message's time on screen. The progress bar, the countdown
 * and the automatic close all read from it, so they can never disagree.
 *
 * It only counts while [run] is executing, which the message does while it is visible. It can be
 * paused and resumed any number of times (the card does so while it is being dragged), and
 * stopped once, for good.
 *
 * @param total the full time on screen. [Duration.INFINITE] makes a timer that never runs.
 * @param timeSource the clock used to credit a wait that is cut short by a pause. The default is
 * the real clock; tests pass their virtual one.
 */
@Stable
internal class HollerTimer(private val total: Duration, private val timeSource: TimeSource = TimeSource.Monotonic) {

    /** `true` when there is a real time limit; `false` for an infinite message. */
    val isFinite: Boolean = total.isFinite()

    /** The full time on screen in nanoseconds, or `0` for an infinite message. */
    private val totalNanos: Long = if (isFinite) {
        total.inWholeNanoseconds
    } else {
        0L
    }

    /** Time counted so far, in nanoseconds. Observable, so drawing code can follow it. */
    private var elapsedNanos: Long by mutableLongStateOf(0L)

    /**
     * Where the timer is in its life. A single value, so the impossible combination "stopped and
     * finished" cannot be represented. Observable, so the card reacts when it changes.
     */
    private var phase: Phase by mutableStateOf(Phase.Counting)

    /** `true` while the timer is held, for example while the card is being dragged. It can resume. */
    val isPaused: Boolean get() = phase == Phase.Paused

    /** `true` once the timer was stopped for good, by the user expanding the message. */
    val isStopped: Boolean get() = phase == Phase.Stopped

    /** `true` once the full time has elapsed. */
    val isFinished: Boolean get() = phase == Phase.Finished

    /**
     * `true` while the timer is still relevant to show: it has a limit and was not stopped.
     * Controls whether the progress bar and the countdown are visible.
     */
    val isActive: Boolean get() = isFinite && phase != Phase.Stopped

    /**
     * The elapsed share of the total time, from `0` to `1`. Read it only while drawing, so that
     * following it causes a redraw and not a recomposition.
     */
    val progress: Float
        get() = if (totalNanos == 0L) {
            0f
        } else {
            (elapsedNanos.toDouble() / totalNanos)
                .toFloat()
                .coerceIn(0f, 1f)
        }

    /**
     * The time left, rounded up to whole seconds. Its value changes once per second, so reading
     * it recomposes the reader once per second and no more.
     */
    val remaining: State<Duration> = derivedStateOf {
        val leftNanos = (totalNanos - elapsedNanos).coerceAtLeast(0L)

        ((leftNanos + NANOS_PER_SECOND - 1) / NANOS_PER_SECOND).seconds
    }

    /**
     * Stops the timer for good. The message then stays until it is closed some other way.
     * Does nothing if the time has already run out.
     */
    fun stop() {
        if (phase == Phase.Counting || phase == Phase.Paused) phase = Phase.Stopped
    }

    /**
     * Holds the timer where it is: the progress bar and the countdown freeze, and the time already
     * counted is kept. Does nothing unless the timer is counting.
     */
    fun pause() {
        if (phase == Phase.Counting) phase = Phase.Paused
    }

    /** Lets a paused timer count again from where it was held. Does nothing unless it is paused. */
    fun resume() {
        if (phase == Phase.Paused) phase = Phase.Counting
    }

    /**
     * Counts time until the limit is reached, the timer is stopped or paused, or the caller is
     * canceled. It can be called again afterward and continues from where it was: a wait that
     * was cut short is credited with the time that really passed, so pausing loses nothing.
     *
     * @param frameDriven `true` to advance on every display frame, which a visible progress bar
     * needs to move smoothly. `false` to advance with plain delays and request no frames at all.
     * @param tickWholeSeconds only used when [frameDriven] is `false`: `true` to advance once per
     * second so a countdown stays current, `false` to wait for the whole remaining time at once.
     */
    suspend fun run(frameDriven: Boolean, tickWholeSeconds: Boolean) {
        if (!isFinite) return

        if (frameDriven) {
            var lastFrameNanos = -1L

            while (phase == Phase.Counting) {
                withFrameNanos { now ->
                    if (lastFrameNanos >= 0L) {
                        advance((now - lastFrameNanos).coerceIn(0L, MAX_FRAME_DELTA_NANOS))
                    }

                    lastFrameNanos = now
                }
            }
        } else {
            while (phase == Phase.Counting) {
                val left = totalNanos - elapsedNanos
                val step = if (tickWholeSeconds) {
                    minOf(left, NANOS_PER_SECOND)
                } else {
                    left
                }

                waitAndCredit(step)
            }
        }
    }

    /**
     * Waits for [stepNanos] and counts it. If the wait is cut short, because the timer was paused or
     * the caller canceled, only the time that really passed is counted.
     *
     * @param stepNanos how long to wait, in nanoseconds.
     */
    private suspend fun waitAndCredit(stepNanos: Long) {
        val started = timeSource.markNow()
        var completed = false

        try {
            delay(stepNanos.nanoseconds)
            completed = true
        } finally {
            advance(
                deltaNanos = if (completed) {
                    stepNanos
                } else {
                    started.elapsedNow().inWholeNanoseconds.coerceIn(0L, stepNanos)
                },
            )
        }
    }

    /**
     * Adds [deltaNanos] to the elapsed time and marks the timer finished when the limit is hit.
     * Does nothing once the timer is stopped or finished. A paused timer still takes the credit for
     * time that passed before the pause, but never finishes while paused: it is held just short of
     * the limit and finishes when it resumes.
     *
     * @param deltaNanos the time that passed, in nanoseconds.
     */
    private fun advance(deltaNanos: Long) {
        if (phase == Phase.Paused) {
            elapsedNanos = (elapsedNanos + deltaNanos).coerceAtMost(totalNanos - 1)
            return
        }

        if (phase != Phase.Counting) return

        elapsedNanos = (elapsedNanos + deltaNanos).coerceAtMost(totalNanos)

        if (elapsedNanos >= totalNanos) phase = Phase.Finished
    }

    /** The stages a timer goes through. It counts, may pause and resume, and ends stopped or finished. */
    private enum class Phase {
        /** Time is passing (or would be, while [run] is executing). */
        Counting,

        /** Held for now; goes back to [Counting] when resumed. */
        Paused,

        /** Stopped for good before the time ran out. */
        Stopped,

        /** The full time has elapsed. */
        Finished,
    }

    /** Constants of the timer. */
    private companion object {

        /** Nanoseconds in one second. */
        const val NANOS_PER_SECOND: Long = 1_000_000_000L

        /**
         * The most time a single frame may add. Frames stop while the app or tab is in the
         * background, so without this cap the first frame after returning would jump the timer.
         */
        const val MAX_FRAME_DELTA_NANOS: Long = 250_000_000L
    }
}
