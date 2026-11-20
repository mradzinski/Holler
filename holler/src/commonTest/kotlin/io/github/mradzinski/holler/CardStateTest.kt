package io.github.mradzinski.holler

import io.github.mradzinski.holler.internal.CardState
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

/** Tests of what a card keeps while it is not drawn. */
class CardStateTest {

    @Test
    fun `a card asking again for the same time on screen gets the timer it had`() {
        val state = CardState()

        val first = state.timerFor(HollerDuration.Custom(3.seconds))
        val second = state.timerFor(HollerDuration.Custom(3.seconds))

        second shouldBeSameInstanceAs first
    }

    @Test
    fun `a card asking for another time on screen gets a new timer`() {
        val state = CardState()

        val first = state.timerFor(HollerDuration.Short)
        val second = state.timerFor(HollerDuration.Long)

        second shouldNotBeSameInstanceAs first
        state.timerFor(HollerDuration.Long) shouldBeSameInstanceAs second
    }

    @Test
    fun `a card drawn again is as expanded as it was left`() {
        val state = CardState()

        state
            .expansionFor(startsExpanded = false)
            .toggle()
            .shouldBeTrue()

        val again = state.expansionFor(startsExpanded = false)

        again.isExpanded.shouldBeTrue()
        again.hintSeen.shouldBeTrue()
        again.hintNudged.shouldBeFalse()
    }

    @Test
    fun `the height of a front card is only known for the width it was measured at`() {
        val state = CardState()

        state.frontHeight(width = 375).shouldBeNull()

        state.recordFrontHeight(width = 375, height = 56)

        state.frontHeight(width = 375) shouldBe 56
        state.frontHeight(width = 320).shouldBeNull()
    }
}
