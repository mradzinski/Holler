package io.github.mradzinski.holler

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue

/**
 * A message that was shown through a [Holler]. Use it to close that message from code.
 *
 * Showing another message with the same [key] replaces the content of this one and returns this same
 * handle, so a handle follows its message through every replacement for as long as it is alive.
 */
@Stable
public interface HollerHandle {

    /**
     * The key of the message: the one passed to `show`, or one made by the library when none was
     * passed. Pass it to `show` to replace the message, or to [Holler.dismiss] to close it.
     */
    public val key: Any

    /**
     * The value the message was shown with, when it was shown with `show(value)`: the app's own
     * object, which the host's `message` gives a look. `null` for a text message. When the message is
     * replaced, this is the value of what replaced it.
     */
    public val value: Any?

    /**
     * Closes the message. If it is on screen it plays its exit animation; if it is still waiting
     * in the queue it is simply removed. A caller waiting in [awaitDismiss] receives
     * [HollerDismissReason.Programmatic]. Calling this more than once does nothing.
     */
    public fun dismiss()

    /**
     * Suspends until the message closes, and returns why. It is how the code that showed a message
     * reacts to how it ended: cancel an upload, retry, or update state the interface reads.
     *
     * ```
     * scope.launch {
     *     val reason = holler.show(Uploading, key = "upload").awaitDismiss()
     *
     *     if (reason != HollerDismissReason.Replaced) upload.cancel()
     * }
     * ```
     *
     * The wait belongs to the caller's coroutine scope. When that scope is cancelled the wait ends and
     * the library forgets the caller at once: nothing of it is kept, and the message stays on screen.
     * That is why there is no callback to pass to `show`: a callback would be held, with everything it
     * captured, for as long as the message lives.
     *
     * - It works whether or not the message ever reached the screen: one closed or replaced while
     *   still waiting in the queue reports too.
     * - When the message is replaced, by a `show` with its key, a wait that had already started
     *   returns [HollerDismissReason.Replaced]. A wait that starts afterwards waits for what replaced
     *   it.
     * - Called after the message has closed, it returns the reason at once.
     * - Several callers can wait for the same message; each gets the reason.
     *
     * @return why the message closed, or [HollerDismissReason.Replaced] if it was replaced.
     */
    public suspend fun awaitDismiss(): HollerDismissReason
}
