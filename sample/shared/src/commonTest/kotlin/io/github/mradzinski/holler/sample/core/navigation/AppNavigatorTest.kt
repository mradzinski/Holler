package io.github.mradzinski.holler.sample.core.navigation

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Tests of the app's back stack. */
class AppNavigatorTest {

    /** The back stack, starting on the inbox. */
    private val navigator = AppNavigator(startDestination = InboxKey)

    @Test
    fun `a new screen goes on top of the back stack`() {
        navigator.navigate(SettingsKey)

        navigator.backStack.toList() shouldBe listOf(InboxKey, SettingsKey)
    }

    @Test
    fun `a screen that is already open is moved to the top and not opened twice`() {
        navigator.navigate(SettingsKey)
        navigator.navigate(PlaygroundKey)
        navigator.navigate(SettingsKey)

        navigator.backStack.toList() shouldBe listOf(InboxKey, PlaygroundKey, SettingsKey)
    }

    @Test
    fun `going back closes the top screen and refuses to close the start screen`() {
        navigator.navigate(SettingsKey)

        navigator.goBack().shouldBeTrue()
        navigator.goBack().shouldBeFalse()
        navigator.backStack.toList() shouldBe listOf(InboxKey)
    }

    @Test
    fun `popping up to a screen closes every screen above it`() {
        navigator.navigate(SettingsKey)
        navigator.navigate(PlaygroundKey)

        navigator.popUpTo { it == InboxKey }.shouldBeTrue()
        navigator.backStack.toList() shouldBe listOf(InboxKey)
    }

    @Test
    fun `popping up to a screen that is not open leaves the back stack as it is`() {
        navigator.navigate(SettingsKey)

        navigator.popUpTo { it == PlaygroundKey }.shouldBeFalse()
        navigator.backStack.toList() shouldBe listOf(InboxKey, SettingsKey)
    }

    @Test
    fun `a back stack survives being saved and restored with its arguments`() {
        navigator.navigate(EmailKey(emailId = 7L))
        val saved = navigator.save()

        val restored = AppNavigator(startDestination = InboxKey)
        restored.restore(saved)

        restored.backStack.toList() shouldBe listOf(InboxKey, EmailKey(emailId = 7L))
    }

    @Test
    fun `restoring from unreadable text leaves the back stack as it is`() {
        navigator.restore("not a back stack")

        navigator.backStack.toList() shouldBe listOf(InboxKey)
    }
}
