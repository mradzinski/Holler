package io.github.mradzinski.holler.sample.data.email

import io.github.mradzinski.holler.sample.data.connectivity.Connectivity
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Keeps the emails in memory and pretends to talk to a server: a refresh and a reply take a moment,
 * and both fail while the app is offline.
 *
 * @param connectivity tells whether the pretend server can be reached.
 * @param scope the app-wide scope a reply is sent in, so it outlives the screen that started it.
 * @param initial the emails the inbox starts with.
 * @param incoming the emails that arrive later, one per successful refresh.
 */
internal class InMemoryEmailRepository(
    private val connectivity: Connectivity,
    private val scope: CoroutineScope,
    initial: List<Email> = sampleInbox(),
    incoming: List<Email> = sampleIncoming(),
) : EmailRepository {

    /** Everything the repository knows, as one value, so each change is a single atomic update. */
    private val mailbox: MutableStateFlow<Mailbox> = MutableStateFlow(
        Mailbox(inbox = initial, incoming = incoming),
    )

    /** Whether a refresh is running. */
    private val refreshing: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /** Where the last reply stands. */
    private val reply: MutableStateFlow<ReplyStatus> = MutableStateFlow(ReplyStatus.Idle)

    /** The emails of the inbox, newest first. */
    override val inbox: Flow<List<Email>> = mailbox
        .map { it.inbox }
        .distinctUntilChanged()

    /** Whether a refresh is running. */
    override val isRefreshing: Flow<Boolean> = refreshing.asStateFlow()

    /** Where the last reply stands. */
    override val replyStatus: Flow<ReplyStatus> = reply.asStateFlow()

    /**
     * Follows one email of the inbox.
     *
     * @param id the id of the email.
     * @return the email and its later changes; `null` while it is not in the inbox.
     */
    override fun observeEmail(id: Long): Flow<Email?> = inbox
        .map { emails -> emails.firstOrNull { it.id == id } }
        .distinctUntilChanged()

    /**
     * Waits as a server would, then delivers the next incoming email if the app is online.
     *
     * @return a failure holding an [OfflineException] when the app is offline.
     */
    override suspend fun refresh(): Result<Unit> {
        refreshing.value = true

        try {
            delay(REFRESH_TIME)

            if (!connectivity.isOnline.first()) return Result.failure(OfflineException())

            mailbox.update { it.withNextIncoming() }

            return Result.success(Unit)
        } finally {
            refreshing.value = false
        }
    }

    /**
     * Marks an email as read.
     *
     * @param id the id of the email.
     */
    override suspend fun markRead(id: Long) {
        mailbox.update { current ->
            current.copy(
                inbox = current.inbox.map { email ->
                    if (email.id == id) {
                        email.copy(isRead = true)
                    } else {
                        email
                    }
                },
            )
        }
    }

    /**
     * Takes an email out of the inbox, remembering where it was.
     *
     * @param id the id of the email.
     */
    override suspend fun delete(id: Long) = mailbox.update {
        it.withDeleted(id)
    }

    /**
     * Puts a deleted email back where it was.
     *
     * @param id the id of the email.
     */
    override suspend fun restore(id: Long) = mailbox.update {
        it.withRestored(id)
    }

    /**
     * Pretends to send a reply in the app-wide scope: sending for a moment, then sent, or failed when offline.
     *
     * @param id the id of the email being answered.
     * @param text the reply, which the pretend server discards.
     */
    override fun sendReply(id: Long, text: String) {
        scope.launch {
            reply.value = ReplyStatus.Sending

            delay(SEND_TIME)

            reply.value = if (connectivity.isOnline.first()) {
                ReplyStatus.Sent
            } else {
                ReplyStatus.Failed
            }
        }
    }

    /** Sets the reply status back to idle, unless a reply is still being sent. */
    override fun acknowledgeReply() = reply.update { status ->
        if (status == ReplyStatus.Sending) {
            status
        } else {
            ReplyStatus.Idle
        }
    }

    /** The constants of this class. */
    private companion object {
        /** How long the pretend server takes to answer a refresh. */
        val REFRESH_TIME: Duration = 800.milliseconds

        /** How long the pretend server takes to accept a reply. */
        val SEND_TIME: Duration = 3.seconds
    }
}

/**
 * The emails the repository holds.
 *
 * @property inbox the emails of the inbox, newest first.
 * @property incoming the emails still to arrive, the next one first.
 * @property deleted the deleted emails by id, each with the place it had in the inbox.
 */
private data class Mailbox(
    val inbox: List<Email>,
    val incoming: List<Email>,
    val deleted: Map<Long, DeletedEmail> = emptyMap(),
) {

    /**
     * Delivers the next incoming email, if there is one.
     *
     * @return the mailbox with that email at the top of the inbox.
     */
    fun withNextIncoming(): Mailbox {
        val next = incoming.firstOrNull() ?: return this

        return copy(
            inbox = listOf(next) + inbox,
            incoming = incoming.drop(1),
        )
    }

    /**
     * Takes an email out of the inbox, remembering where it was.
     *
     * @param id the id of the email.
     * @return the mailbox without that email, or this mailbox when the email is not in the inbox.
     */
    fun withDeleted(id: Long): Mailbox {
        val index = inbox.indexOfFirst { it.id == id }

        if (index < 0) return this

        return copy(
            inbox = inbox - inbox[index],
            deleted = deleted + (id to DeletedEmail(inbox[index], index)),
        )
    }

    /**
     * Puts a deleted email back where it was.
     *
     * @param id the id of the email.
     * @return the mailbox with that email back, or this mailbox when the email was not deleted.
     */
    fun withRestored(id: Long): Mailbox {
        val entry = deleted[id] ?: return this
        val index = entry.index.coerceAtMost(inbox.size)

        return copy(
            inbox = inbox.take(index) + entry.email + inbox.drop(index),
            deleted = deleted - id,
        )
    }
}

/**
 * A deleted email and where it was.
 *
 * @property email the email.
 * @property index its place in the inbox when it was deleted.
 */
private data class DeletedEmail(val email: Email, val index: Int)
