package io.github.mradzinski.holler.sample.ui.email

import io.github.mradzinski.holler.Holler
import io.github.mradzinski.holler.sample.core.navigation.AppNavigator
import io.github.mradzinski.holler.sample.core.navigation.EmailKey
import io.github.mradzinski.holler.sample.core.navigation.InboxKey
import io.github.mradzinski.holler.sample.data.email.EmailRepository
import io.github.mradzinski.holler.sample.data.email.ReplyStatus
import io.github.mradzinski.holler.sample.fake.FIRST_EMAIL
import io.github.mradzinski.holler.sample.fake.FakeConnectivity
import io.github.mradzinski.holler.sample.fake.MainDispatcherTest
import io.github.mradzinski.holler.sample.fake.SECOND_EMAIL
import io.github.mradzinski.holler.sample.fake.testEmailRepository
import io.github.mradzinski.holler.sample.messages.AppMessage
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** Tests of the email screen's view model, with the real in-memory repository and navigator. */
@OptIn(ExperimentalCoroutinesApi::class)
class EmailViewModelTest : MainDispatcherTest() {

    /** The host state the view model shows its messages through. No host draws it in these tests. */
    private val holler = Holler()

    /** The back stack, with the first email open over the inbox. */
    private val navigator = AppNavigator(startDestination = InboxKey).apply {
        navigate(EmailKey(FIRST_EMAIL.id))
    }

    @Test
    fun `opening the screen shows the email and marks it as read`() = runTest {
        val emails = testEmailRepository(FakeConnectivity(), scope = this)

        val viewModel = viewModel(emails)

        viewModel.state.value.email?.subject shouldBe FIRST_EMAIL.subject
        emails.inbox.first().first { it.id == FIRST_EMAIL.id }.isRead.shouldBeTrue()
    }

    @Test
    fun `delete removes the email and asks for the email deleted message and closes the screen`() = runTest {
        val emails = testEmailRepository(FakeConnectivity(), scope = this)
        val viewModel = viewModel(emails)

        viewModel.onEvent(EmailEvent.OnDeleteClick)

        emails.inbox.first() shouldBe listOf(SECOND_EMAIL)
        holler.messages.map { it.value } shouldBe listOf(AppMessage.EmailDeleted(FIRST_EMAIL.id))
        navigator.backStack.toList() shouldBe listOf(InboxKey)
    }

    @Test
    fun `the state keeps the deleted email so the closing screen is not blank`() = runTest {
        val emails = testEmailRepository(FakeConnectivity(), scope = this)
        val viewModel = viewModel(emails)

        viewModel.onEvent(EmailEvent.OnDeleteClick)

        viewModel.state.value.email?.subject shouldBe FIRST_EMAIL.subject
    }

    @Test
    fun `send hands the reply to the repository which sends it`() = runTest {
        val emails = testEmailRepository(FakeConnectivity(), scope = this)
        val viewModel = viewModel(emails)

        viewModel.onEvent(EmailEvent.OnSendClick(text = "Thanks"))
        advanceUntilIdle()

        emails.replyStatus.first() shouldBe ReplyStatus.Sent
    }

    @Test
    fun `the Back arrow closes the screen`() = runTest {
        val viewModel = viewModel(testEmailRepository(FakeConnectivity(), scope = this))

        viewModel.onEvent(EmailEvent.OnBackClick)

        navigator.backStack.toList() shouldBe listOf(InboxKey)
    }

    /**
     * Builds the view model of [FIRST_EMAIL]'s screen and starts watching its state, as the screen would.
     *
     * @param emails the repository the view model uses.
     * @return the view model.
     */
    private fun TestScope.viewModel(emails: EmailRepository): EmailViewModel {
        val viewModel = EmailViewModel(FIRST_EMAIL.id, emails, holler, navigator)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

        return viewModel
    }
}
