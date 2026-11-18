package io.github.mradzinski.holler.sample.ui.inbox

import app.cash.turbine.test
import io.github.mradzinski.holler.Holler
import io.github.mradzinski.holler.sample.core.input.DeviceInput
import io.github.mradzinski.holler.sample.core.navigation.AppNavigator
import io.github.mradzinski.holler.sample.core.navigation.EmailKey
import io.github.mradzinski.holler.sample.core.navigation.InboxKey
import io.github.mradzinski.holler.sample.core.navigation.SettingsKey
import io.github.mradzinski.holler.sample.fake.FIRST_EMAIL
import io.github.mradzinski.holler.sample.fake.FakeConnectivity
import io.github.mradzinski.holler.sample.fake.INCOMING_EMAIL
import io.github.mradzinski.holler.sample.fake.MainDispatcherTest
import io.github.mradzinski.holler.sample.fake.SECOND_EMAIL
import io.github.mradzinski.holler.sample.fake.testEmailRepository
import io.github.mradzinski.holler.sample.messages.AppMessage
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
import kotlinx.coroutines.test.runTest

/** Tests of the inbox screen's view model, with the real in-memory repository and navigator. */
@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModelTest : MainDispatcherTest() {

    /** The connection the test switches on and off. */
    private val connectivity = FakeConnectivity()

    /** The host state the view model shows its messages through. No host draws it in these tests. */
    private val holler = Holler()

    /** The back stack, starting on the inbox. */
    private val navigator = AppNavigator(startDestination = InboxKey)

    @Test
    fun `the state has one row per email each marked unread until it is opened`() = runTest {
        val viewModel = viewModel()

        val rows = viewModel.state.value.emails

        rows.map { it.id } shouldBe listOf(FIRST_EMAIL.id, SECOND_EMAIL.id)
        rows.all { it.isUnread }.shouldBeTrue()
    }

    @Test
    fun `a refresh that works shows the new email and asks for no message`() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(InboxEvent.OnRefreshPull)
        advanceUntilIdle()

        viewModel.state.value.emails.first().id shouldBe INCOMING_EMAIL.id
        holler.messages.shouldBeEmpty()
    }

    @Test
    fun `a device with a touch screen gets no refresh button`() = runTest {
        val viewModel = viewModel(hasTouchScreen = true)

        viewModel.state.value.showRefreshButton.shouldBeFalse()
    }

    @Test
    fun `a device without a touch screen gets a refresh button that checks for new emails`() = runTest {
        val viewModel = viewModel(hasTouchScreen = false)

        viewModel.state.value.showRefreshButton.shouldBeTrue()

        viewModel.onEvent(InboxEvent.OnRefreshClick)
        advanceUntilIdle()

        viewModel.state.value.emails.first().id shouldBe INCOMING_EMAIL.id
    }

    @Test
    fun `the state says a refresh is running until the new email is in`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitItem().isRefreshing.shouldBeFalse()

            viewModel.onEvent(InboxEvent.OnRefreshPull)

            awaitItem().isRefreshing.shouldBeTrue()

            val refreshed = awaitItem()

            refreshed.isRefreshing.shouldBeFalse()
            refreshed.emails.first().id shouldBe INCOMING_EMAIL.id
        }
    }

    @Test
    fun `a refresh that fails asks for the could not refresh message`() = runTest {
        val viewModel = viewModel()
        connectivity.isOnline.value = false

        viewModel.onEvent(InboxEvent.OnRefreshPull)
        advanceUntilIdle()

        holler.messages.map { it.value } shouldBe listOf(AppMessage.RefreshFailed)
    }

    @Test
    fun `pressing a row opens that email on top of the inbox`() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(InboxEvent.OnEmailClick(FIRST_EMAIL.id))

        navigator.backStack.toList() shouldBe listOf(InboxKey, EmailKey(FIRST_EMAIL.id))
    }

    @Test
    fun `pressing a second row swaps the open email so Back leads to the inbox alone`() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(InboxEvent.OnEmailClick(FIRST_EMAIL.id))
        viewModel.onEvent(InboxEvent.OnEmailClick(SECOND_EMAIL.id))

        navigator.backStack.toList() shouldBe listOf(InboxKey, EmailKey(SECOND_EMAIL.id))
    }

    @Test
    fun `swiping a row away deletes its email and asks for the email deleted message`() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(InboxEvent.OnEmailSwipe(FIRST_EMAIL.id))

        viewModel.state.value.emails.map { it.id } shouldBe listOf(SECOND_EMAIL.id)
        holler.messages.map { it.value } shouldBe listOf(AppMessage.EmailDeleted(FIRST_EMAIL.id))
    }

    @Test
    fun `swiping away the email that is open next to the inbox closes its screen as well`() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(InboxEvent.OnEmailClick(FIRST_EMAIL.id))

        viewModel.onEvent(InboxEvent.OnEmailSwipe(FIRST_EMAIL.id))

        navigator.backStack.toList() shouldBe listOf(InboxKey)
    }

    @Test
    fun `swiping away another email leaves the open one on screen`() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(InboxEvent.OnEmailClick(FIRST_EMAIL.id))

        viewModel.onEvent(InboxEvent.OnEmailSwipe(SECOND_EMAIL.id))

        navigator.backStack.toList() shouldBe listOf(InboxKey, EmailKey(FIRST_EMAIL.id))
    }

    @Test
    fun `the settings button opens the settings screen`() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(InboxEvent.OnSettingsClick)

        navigator.backStack.toList() shouldBe listOf(InboxKey, SettingsKey)
    }

    /**
     * Builds the view model and starts watching its state, as the screen would.
     *
     * @param hasTouchScreen whether the device the app runs on has a touch screen.
     * @return the view model.
     */
    private fun TestScope.viewModel(hasTouchScreen: Boolean = true): InboxViewModel {
        val viewModel = InboxViewModel(
            emails = testEmailRepository(connectivity, scope = this),
            holler = holler,
            navigator = navigator,
            input = DeviceInput(hasTouchScreen),
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

        return viewModel
    }
}
