package io.github.mradzinski.holler.sample.data.email

import app.cash.turbine.test
import io.github.mradzinski.holler.sample.fake.FIRST_EMAIL
import io.github.mradzinski.holler.sample.fake.FakeConnectivity
import io.github.mradzinski.holler.sample.fake.INCOMING_EMAIL
import io.github.mradzinski.holler.sample.fake.SECOND_EMAIL
import io.github.mradzinski.holler.sample.fake.testEmailRepository
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/** Tests of the in-memory email repository, on virtual time. */
@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryEmailRepositoryTest {

    /** The connection the test switches on and off. */
    private val connectivity = FakeConnectivity()

    @Test
    fun `undoing a delete puts the email back in its place and not at the end`() = runTest {
        val emails = testEmailRepository(connectivity, scope = this)

        emails.delete(FIRST_EMAIL.id)

        emails.inbox.first() shouldBe listOf(SECOND_EMAIL)

        emails.restore(FIRST_EMAIL.id)

        emails.inbox.first() shouldBe listOf(FIRST_EMAIL, SECOND_EMAIL)
    }

    @Test
    fun `restoring an email that is still in the inbox does not add it twice`() = runTest {
        val emails = testEmailRepository(connectivity, scope = this)

        emails.restore(FIRST_EMAIL.id)

        emails.inbox.first() shouldBe listOf(FIRST_EMAIL, SECOND_EMAIL)
    }

    @Test
    fun `a refresh while online adds the next email at the top with the indicator on while it runs`() = runTest {
        val emails = testEmailRepository(connectivity, scope = this)

        emails.isRefreshing.test {
            awaitItem().shouldBeFalse()

            val result = emails.refresh()

            awaitItem().shouldBeTrue()
            awaitItem().shouldBeFalse()
            result.isSuccess.shouldBeTrue()
        }

        emails.inbox.first() shouldBe listOf(INCOMING_EMAIL, FIRST_EMAIL, SECOND_EMAIL)
    }

    @Test
    fun `a refresh while offline fails and leaves the inbox alone with the indicator on meanwhile`() = runTest {
        val emails = testEmailRepository(connectivity, scope = this)
        connectivity.isOnline.value = false

        emails.isRefreshing.test {
            awaitItem().shouldBeFalse()

            val result = emails.refresh()

            awaitItem().shouldBeTrue()
            awaitItem().shouldBeFalse()
            result.exceptionOrNull().shouldBeInstanceOf<OfflineException>()
        }

        emails.inbox.first() shouldBe listOf(FIRST_EMAIL, SECOND_EMAIL)
    }

    @Test
    fun `marking one email as read leaves the others unread`() = runTest {
        val emails = testEmailRepository(connectivity, scope = this)

        emails.markRead(SECOND_EMAIL.id)

        emails.inbox.first() shouldBe listOf(FIRST_EMAIL, SECOND_EMAIL.copy(isRead = true))
    }

    @Test
    fun `a reply is sending then sent then idle once its result was seen`() = runTest {
        val emails = testEmailRepository(connectivity, scope = this)

        emails.replyStatus.test {
            awaitItem() shouldBe ReplyStatus.Idle

            emails.sendReply(FIRST_EMAIL.id, text = "Thanks")

            awaitItem() shouldBe ReplyStatus.Sending
            awaitItem() shouldBe ReplyStatus.Sent

            emails.acknowledgeReply()

            awaitItem() shouldBe ReplyStatus.Idle
        }
    }

    @Test
    fun `a reply sent while offline ends as failed`() = runTest {
        val emails = testEmailRepository(connectivity, scope = this)
        connectivity.isOnline.value = false

        emails.sendReply(FIRST_EMAIL.id, text = "Thanks")
        advanceUntilIdle()

        emails.replyStatus.first() shouldBe ReplyStatus.Failed
    }

    @Test
    fun `acknowledging a result does not reset a reply that is still on its way`() = runTest {
        val emails = testEmailRepository(connectivity, scope = this)

        emails.sendReply(FIRST_EMAIL.id, text = "Thanks")
        runCurrent()
        emails.acknowledgeReply()

        emails.replyStatus.first() shouldBe ReplyStatus.Sending
    }
}
