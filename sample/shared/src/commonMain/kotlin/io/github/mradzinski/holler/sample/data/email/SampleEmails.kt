package io.github.mradzinski.holler.sample.data.email

// The made-up emails of the sample. They are data, not interface text, so they are written here and
// not in the string resources.

/** The id given to the first email that arrives later. Ids of the starting inbox are below it. */
private const val FIRST_INCOMING_ID: Long = 100L

/**
 * The emails the inbox starts with.
 *
 * @return the emails, newest first.
 */
internal fun sampleInbox(): List<Email> = listOf(
    Email(
        id = 1L,
        sender = "Ana Flores",
        subject = "Design review moved to Thursday",
        body = "Hi! The design review is moving from Wednesday to Thursday at 10:00, same room.\n\n" +
            "Bring the latest prototype if you can. We will go through the onboarding flow first.",
    ),
    Email(
        id = 2L,
        sender = "Build bot",
        subject = "Nightly build passed",
        body = "All 412 checks passed on main.\n\nDuration: 14 minutes. No new warnings since the last build.",
        isRead = true,
    ),
    Email(
        id = 3L,
        sender = "Tomás Ruiz",
        subject = "Lunch on Friday?",
        body = "A few of us are trying the new place on the corner this Friday at 13:00.\n\nWant to come?",
    ),
    Email(
        id = 4L,
        sender = "Billing",
        subject = "Your invoice for October",
        body = "Your invoice for October is ready.\n\nAmount due: 24.00. It will be charged to the card on file on the 5th.",
        isRead = true,
    ),
    Email(
        id = 5L,
        sender = "Lucía Méndez",
        subject = "Notes from the planning session",
        body = "Thanks for joining today. The short version:\n\n" +
            "1. The release moves one week.\n2. Search goes first, offline mode second.\n3. We meet again on Monday.",
    ),
)

/**
 * The emails that arrive later: each successful refresh delivers the next one.
 *
 * @return the emails, in the order they arrive.
 */
internal fun sampleIncoming(): List<Email> = listOf(
    Email(
        id = FIRST_INCOMING_ID,
        sender = "Ana Flores",
        subject = "Re: Design review moved to Thursday",
        body = "One more thing: the room changed too. We are in the small one on the second floor.",
    ),
    Email(
        id = FIRST_INCOMING_ID + 1,
        sender = "Calendar",
        subject = "Reminder: planning on Monday at 9:30",
        body = "This is a reminder for the planning session on Monday at 9:30.",
    ),
)
