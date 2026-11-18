package io.github.mradzinski.holler.sample.data.email

import kotlinx.coroutines.flow.Flow

/** The user's emails, and what can be done with them. */
internal interface EmailRepository {

    /** The emails of the inbox, newest first, and every later change to them. */
    val inbox: Flow<List<Email>>

    /** Whether a [refresh] is running. */
    val isRefreshing: Flow<Boolean>

    /** Where the last reply stands. It goes back to [ReplyStatus.Idle] with [acknowledgeReply]. */
    val replyStatus: Flow<ReplyStatus>

    /**
     * Follows one email.
     *
     * @param id the id of the email.
     * @return the email and its later changes; `null` while it is not in the inbox.
     */
    fun observeEmail(id: Long): Flow<Email?>

    /**
     * Checks for new emails.
     *
     * @return a failure holding an [OfflineException] when the app is offline.
     */
    suspend fun refresh(): Result<Unit>

    /**
     * Marks an email as read.
     *
     * @param id the id of the email.
     */
    suspend fun markRead(id: Long)

    /**
     * Removes an email from the inbox. It can be put back with [restore].
     *
     * @param id the id of the email.
     */
    suspend fun delete(id: Long)

    /**
     * Puts a deleted email back where it was in the inbox.
     *
     * @param id the id of the email.
     */
    suspend fun restore(id: Long)

    /**
     * Starts sending a reply and returns at once. The sending goes on after the screen that started it
     * closes; follow it with [replyStatus].
     *
     * @param id the id of the email being answered.
     * @param text the reply.
     */
    fun sendReply(id: Long, text: String)

    /** Tells the repository the result of the last reply was seen, which sets [replyStatus] back to idle. */
    fun acknowledgeReply()
}
