package io.github.mradzinski.holler.sample.fake

import io.github.mradzinski.holler.sample.data.connectivity.Connectivity
import io.github.mradzinski.holler.sample.data.email.Email
import io.github.mradzinski.holler.sample.data.email.InMemoryEmailRepository
import kotlinx.coroutines.CoroutineScope

/** The first email of the test inbox, unread. */
internal val FIRST_EMAIL: Email = Email(id = 1L, sender = "Ana", subject = "First", body = "One")

/** The second email of the test inbox, unread. */
internal val SECOND_EMAIL: Email = Email(id = 2L, sender = "Tomás", subject = "Second", body = "Two")

/** The email a successful refresh delivers in tests. */
internal val INCOMING_EMAIL: Email = Email(id = 3L, sender = "Lucía", subject = "Third", body = "Three")

/**
 * Builds the app's own in-memory repository with a small, known inbox. Tests use the real one: it is
 * already in memory, and its delays run on the test's virtual time.
 *
 * @param connectivity tells the repository whether it is online.
 * @param scope the scope replies are sent in.
 * @return the repository, holding [FIRST_EMAIL] and [SECOND_EMAIL], with [INCOMING_EMAIL] still to arrive.
 */
internal fun testEmailRepository(connectivity: Connectivity, scope: CoroutineScope): InMemoryEmailRepository = InMemoryEmailRepository(
    connectivity = connectivity,
    scope = scope,
    initial = listOf(FIRST_EMAIL, SECOND_EMAIL),
    incoming = listOf(INCOMING_EMAIL),
)
