package io.github.mradzinski.holler

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * How long a message stays on screen before it closes itself.
 *
 * The values are the same on every platform so a message behaves identically everywhere.
 *
 * @param duration the time on screen. It is private on purpose: callers pick one of the
 * subclasses instead of reading the raw value.
 */
public sealed class HollerDuration(private val duration: Duration) {

    /**
     * The raw time on screen, for the library's own use.
     * [Duration.INFINITE] means the message never closes by itself.
     */
    internal val value: Duration get() = duration

    /** A short message: 5 seconds on every platform. This is the default. */
    public data object Short : HollerDuration(5.seconds)

    /** A long message: 10 seconds on every platform. */
    public data object Long : HollerDuration(10.seconds)

    /**
     * The message never closes by itself. No progress bar or countdown is shown,
     * because there is no timer to show or stop.
     */
    public data object Infinite : HollerDuration(Duration.INFINITE)

    /**
     * Any time on screen chosen by the caller. Two custom durations are equal when they last the
     * same time.
     *
     * @param duration the time on screen. Must be greater than zero. Passing
     * [Duration.INFINITE] behaves exactly like [Infinite].
     * @throws IllegalArgumentException if [duration] is zero or negative.
     */
    public data class Custom(private val duration: Duration) : HollerDuration(duration) {

        init {
            require(duration.isPositive()) {
                "A message duration must be greater than zero, was $duration"
            }
        }
    }
}
