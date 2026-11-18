package io.github.mradzinski.holler

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Tests of the time-on-screen values. */
class HollerDurationTest {

    @Test
    fun `named durations have the agreed platform-independent values`() {
        HollerDuration.Short.value shouldBe 5.seconds
        HollerDuration.Long.value shouldBe 10.seconds
        HollerDuration.Infinite.value shouldBe Duration.INFINITE
    }

    @Test
    fun `a custom duration keeps its value and compares by it`() {
        HollerDuration.Custom(3.seconds).value shouldBe 3.seconds
        HollerDuration.Custom(3.seconds) shouldBe HollerDuration.Custom(3.seconds)
        HollerDuration.Custom(4.seconds) shouldNotBe HollerDuration.Custom(3.seconds)
    }

    @Test
    fun `a custom duration has a hash consistent with equality and a readable description`() {
        HollerDuration.Custom(3.seconds).hashCode() shouldBe HollerDuration.Custom(3.seconds).hashCode()
        HollerDuration.Custom(3.seconds).toString() shouldBe "Custom(duration=3s)"
    }

    @Test
    fun `zero and negative custom durations are rejected`() {
        shouldThrow<IllegalArgumentException> { HollerDuration.Custom(Duration.ZERO) }
        shouldThrow<IllegalArgumentException> { HollerDuration.Custom((-1).seconds) }
    }

    @Test
    fun `a copy with another duration is a new custom duration checked like any other`() {
        val custom = HollerDuration.Custom(3.seconds)

        custom.copy(duration = 4.seconds) shouldBe HollerDuration.Custom(4.seconds)
        shouldThrow<IllegalArgumentException> { custom.copy(duration = Duration.ZERO) }
    }
}
