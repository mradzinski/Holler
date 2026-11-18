package io.github.mradzinski.holler.sample.app

import app.cash.turbine.test
import io.github.mradzinski.holler.Holler
import io.github.mradzinski.holler.sample.core.navigation.AppNavigator
import io.github.mradzinski.holler.sample.core.navigation.EmailKey
import io.github.mradzinski.holler.sample.core.navigation.InboxKey
import io.github.mradzinski.holler.sample.data.email.EmailRepository
import io.github.mradzinski.holler.sample.data.email.ReplyStatus
import io.github.mradzinski.holler.sample.data.settings.InMemorySettingsRepository
import io.github.mradzinski.holler.sample.fake.FIRST_EMAIL
import io.github.mradzinski.holler.sample.fake.FakeConnectivity
import io.github.mradzinski.holler.sample.fake.MainDispatcherTest
import io.github.mradzinski.holler.sample.fake.SECOND_EMAIL
import io.github.mradzinski.holler.sample.fake.testEmailRepository
import io.github.mradzinski.holler.sample.messages.AppMessage
import io.github.mradzinski.holler.sample.messages.REPLY_KEY
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/** Tests of the app root's view model: the state it shares, the lasting messages and the actions of the messages. */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest : MainDispatcherTest() {

    /** The settings the test changes. */
    private val settings = InMemorySettingsRepository()

    /** The connection the test switches on and off. */
    private val connectivity = FakeConnectivity()

    /** The host state the view model shows its messages through. No host draws it in these tests. */
    private val holler = Holler()

    /** The navigator the view model uses, with an email open over the inbox. */
    private val navigator = AppNavigator(startDestination = InboxKey).apply {
        navigate(EmailKey(FIRST_EMAIL.id))
    }

    @Test
    fun `the state carries the theme and the look and position of messages from the settings`() = runTest {
        val viewModel = viewModel(testEmailRepository(connectivity, scope = this))

        settings.update {
            it.copy(
                darkTheme = true,
                brandLook = true,
                messagesAtTop = true,
                messagesAsPile = true,
            )
        }

        val expected = AppUiState(
            darkTheme = true,
            brandLook = true,
            messagesAtTop = true,
            messagesAsPile = true,
        )
        viewModel.state.value shouldBe expected
    }

    @Test
    fun `the state says the app is offline for as long as the connection is lost`() = runTest {
        val viewModel = viewModel(testEmailRepository(connectivity, scope = this))

        viewModel.state.test {
            awaitItem().offline.shouldBeFalse()

            connectivity.isOnline.value = false

            awaitItem().offline.shouldBeTrue()

            connectivity.isOnline.value = true

            awaitItem().offline.shouldBeFalse()
        }
    }

    @Test
    fun `the offline message shows when the connection is lost and closes early if it comes back`() = runTest {
        viewModel(testEmailRepository(connectivity, scope = this))

        shownMessages().shouldBeEmpty()

        connectivity.isOnline.value = false

        shownMessages() shouldBe listOf(AppMessage.Offline)

        connectivity.isOnline.value = true

        shownMessages().shouldBeEmpty()
    }

    @Test
    fun `the sending message is replaced by the sent one and the reply is then acknowledged`() = runTest {
        val emails = testEmailRepository(connectivity, scope = this)
        viewModel(emails)

        emails.sendReply(FIRST_EMAIL.id, text = "Thanks")
        runCurrent()

        shownMessages() shouldBe listOf(AppMessage.ReplySending)

        advanceUntilIdle()

        shownMessages() shouldBe listOf(AppMessage.ReplySent)
        emails.replyStatus.first() shouldBe ReplyStatus.Idle
    }

    @Test
    fun `sending a reply closes the email and goes back to the inbox`() = runTest {
        val emails = testEmailRepository(connectivity, scope = this)
        viewModel(emails)

        navigator.backStack shouldBe listOf(InboxKey, EmailKey(FIRST_EMAIL.id))

        emails.sendReply(FIRST_EMAIL.id, text = "Thanks")
        runCurrent()

        navigator.backStack shouldBe listOf(InboxKey)

        advanceUntilIdle()
    }

    @Test
    fun `a sent reply is not shown once its sending message was closed but is still acknowledged`() = runTest {
        val emails = testEmailRepository(connectivity, scope = this)
        viewModel(emails)

        emails.sendReply(FIRST_EMAIL.id, text = "Thanks")
        runCurrent()
        holler.dismiss(REPLY_KEY)

        advanceUntilIdle()

        shownMessages().shouldBeEmpty()
        emails.replyStatus.first() shouldBe ReplyStatus.Idle
    }

    @Test
    fun `closing one sending message does not hide the result of the next reply`() = runTest {
        val emails = testEmailRepository(connectivity, scope = this)
        viewModel(emails)

        emails.sendReply(FIRST_EMAIL.id, text = "Thanks")
        runCurrent()
        holler.dismiss(REPLY_KEY)
        advanceUntilIdle()

        emails.sendReply(FIRST_EMAIL.id, text = "Thanks again")
        advanceUntilIdle()

        shownMessages() shouldBe listOf(AppMessage.ReplySent)
    }

    @Test
    fun `a reply that cannot be sent ends as not sent next to the offline message`() = runTest {
        val emails = testEmailRepository(connectivity, scope = this)
        viewModel(emails)
        connectivity.isOnline.value = false

        emails.sendReply(FIRST_EMAIL.id, text = "Thanks")
        advanceUntilIdle()

        shownMessages() shouldBe listOf(AppMessage.Offline, AppMessage.ReplyFailed)
    }

    @Test
    fun `undo on the email deleted message restores the email`() = runTest {
        val emails = testEmailRepository(connectivity, scope = this)
        val viewModel = viewModel(emails)
        emails.delete(FIRST_EMAIL.id)

        viewModel.onEvent(AppEvent.OnUndoDeleteClick(FIRST_EMAIL.id))

        emails.inbox.first() shouldBe listOf(FIRST_EMAIL, SECOND_EMAIL)
    }

    @Test
    fun `a retry that fails again shows the same message once more`() = runTest {
        val viewModel = viewModel(testEmailRepository(connectivity, scope = this))
        connectivity.isOnline.value = false

        viewModel.onEvent(AppEvent.OnRetryRefreshClick)
        advanceUntilIdle()

        shownMessages() shouldBe listOf(AppMessage.Offline, AppMessage.RefreshFailed)
    }

    /**
     * Reads what the view model has shown and not closed.
     *
     * @return the values of the messages in the host state, oldest first.
     */
    private fun shownMessages(): List<Any?> = holler.messages.map {
        it.value
    }

    /**
     * Builds the view model and starts watching its state, as the app's root would.
     *
     * @param emails the repository the view model uses.
     * @return the view model.
     */
    private fun TestScope.viewModel(emails: EmailRepository): AppViewModel {
        val viewModel = AppViewModel(settings, connectivity, emails, holler, navigator)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

        return viewModel
    }
}
