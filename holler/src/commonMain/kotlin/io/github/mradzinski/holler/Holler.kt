package io.github.mradzinski.holler

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import io.github.mradzinski.holler.internal.HollerContent
import io.github.mradzinski.holler.internal.HollerEntry
import io.github.mradzinski.holler.internal.TextMessage
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Holds the messages of a [HollerHost] and is the entry point for showing them. Create one, give it
 * to a [HollerHost] placed above your content, and call [show] from anywhere: no coroutine is needed.
 * Inside a composable, keep it with `remember { Holler() }`.
 *
 * Messages beyond the host's `maxVisible` wait in a queue. A waiting message's timer starts when
 * it becomes visible, not when [show] is called.
 *
 * Every message has a key, passed to [show] or made by the library. Showing a message with the key
 * of one that is still alive replaces that message where it is, and [dismiss] closes a message by
 * its key.
 *
 * It is a plain class that needs no composition, so it can also be created outside the interface, for
 * example as a singleton in the app's dependency injection, and shared between the host and the code
 * that shows messages. Such code can show a message as one of the app's own objects, with
 * `show(value)`, and leave its look to the host. A message shown before the host is on screen waits
 * in the queue.
 *
 * It can be called from any thread, so a view model may show or close a message from a background
 * coroutine. To react to how a message ended, wait for it with [HollerHandle.awaitDismiss]: there is
 * no callback to pass, so the library never holds on to the code that showed a message.
 */
@Stable
public class Holler {

    /** All messages, visible and queued, oldest first. Only changed while holding [lock]. */
    private val queue: MutableList<HollerEntry> = mutableStateListOf()

    /** All messages, visible and queued, oldest first, to read only. Observable, so the host follows it. */
    internal val entries: List<HollerEntry> get() = queue

    /**
     * Guards the queue and the stage of every message in it, so the state can be called from any
     * thread. No callback of the app runs while it is held.
     */
    internal val lock: SynchronizedObject = SynchronizedObject()

    /** Source of the unique numbers given to messages. Only touched while holding [lock]. */
    private var nextId: Long = 0L

    /**
     * The messages that are on screen or waiting in the queue, oldest first. A message that is closing
     * is left out. Each one gives its [HollerHandle.key] and, when it was shown as a value, its
     * [HollerHandle.value], which is what lets a test check what was shown without drawing anything.
     */
    public val messages: List<HollerHandle>
        get() = synchronized(lock) { entries.filterNot { it.isDismissed } }

    /**
     * Shows a message as one of the app's own objects, with no look attached. The [HollerHost] of this
     * object decides how it looks: its `message` lambda receives the value and draws it with `HollerMessage(…)`.
     * That keeps the code that shows a message, a view model for example, free of anything visual.
     *
     * The value can be anything: typically a `data class` or `data object` of a sealed type the app
     * defines for its messages. If the host draws nothing for it, the message is dropped.
     *
     * @param value the app's own object describing the message.
     * @param key identifies the message, so it can be replaced or closed later. Any value with a
     * meaningful `equals` works. If a message with an equal key is on screen or waiting in the queue,
     * this call replaces it where it is, with no exit or enter animation. A message that is already
     * closing is not replaced: the new one is shown as a new message. `null` (the default) gives the
     * message a key of its own, so it never replaces another.
     * @return a handle that can close the message from code, wait for it to close and tell why, and
     * holds its key and value. When the call replaced a message, it is the handle that message already had.
     */
    public fun show(value: Any, key: Any? = null): HollerHandle {
        // `show("Saved", HollerDuration.Long)` compiles as a value with the duration as its key, and
        // would draw nothing. A duration is never a sensible key, so say what went wrong.
        require(key !is HollerDuration) {
            "A HollerDuration was passed as the key of a message. To show text for a given time, name the argument: " +
                "show(title, duration = …)."
        }

        return enqueue(content = HollerContent(value), key = key)
    }

    /**
     * Shows a message of plain text, drawn with the look of the [HollerHost]: its colors, style and
     * animation. It is the short way for a message that has nothing to decide about its look. For
     * anything more (an icon, actions, a countdown, its own colors), show the message as a value with
     * [show] and give it a look in the host.
     *
     * Name the arguments after the title: `show("Saved", duration = HollerDuration.Long)`. A second
     * argument without a name that is not text would be taken as the `key` of a value.
     *
     * @param title the short message, always visible. It is not truncated: keep it short.
     * @param details the long text, revealed by expanding the card. `null` (the default) means the
     * card cannot be expanded and has no expand button.
     * @param duration how long the message stays before closing itself. Defaults to
     * [HollerDuration.Short]. With [HollerDuration.Infinite] it never closes by itself.
     * @param key identifies the message, so it can be replaced or closed later. Any value with a
     * meaningful `equals` works. If a message with an equal key is on screen or waiting in the queue,
     * this call replaces it where it is, with no exit or enter animation. `null` (the default) gives
     * the message a key of its own, so it never replaces another.
     * @return a handle that can close the message from code, wait for it to close and tell why, and
     * holds its key. Its
     * [HollerHandle.value] is `null`. When the call replaced a message, it is the handle that message
     * already had.
     */
    public fun show(
        title: String,
        details: String? = null,
        duration: HollerDuration = HollerDuration.Short,
        key: Any? = null,
    ): HollerHandle = enqueue(
        content = HollerContent(TextMessage(title, details, duration)),
        key = key,
    )

    /**
     * Closes the message that has the given [key], on screen or waiting in the queue. A caller
     * waiting in [HollerHandle.awaitDismiss] receives [HollerDismissReason.Programmatic]. Does nothing when no message has that key
     * or the message is already closing.
     *
     * The key is the value passed to [show] or the [HollerHandle.key] of the handle it returned, not
     * the handle itself: to close through a handle, call [HollerHandle.dismiss].
     *
     * @param key the key of the message to close.
     */
    public fun dismiss(key: Any) {
        synchronized(lock) { find(key) }?.dismiss(HollerDismissReason.Programmatic)
    }

    /**
     * Closes every message, visible or queued. Callers waiting in [HollerHandle.awaitDismiss] receive
     * [HollerDismissReason.Programmatic].
     */
    public fun dismissAll() {
        synchronized(lock) { entries.toList() }.forEach { it.dismiss(HollerDismissReason.Programmatic) }
    }

    /**
     * Forgets a message completely. Called once its exit animation has finished, or directly
     * when it is closed while still queued.
     *
     * @param entry the message to forget.
     */
    internal fun remove(entry: HollerEntry) {
        synchronized(lock) { queue.remove(entry) }
    }

    /**
     * Puts content in the queue: in place of the message that has [key], or as a new message.
     *
     * @param content what the message shows.
     * @param key the caller's key, or `null` for a message that gets a key of its own.
     * @return the message that now shows [content].
     */
    private fun enqueue(content: HollerContent, key: Any?): HollerEntry {
        var reportReplaced: (() -> Unit)? = null

        // Looking for the key and then replacing or adding is one step, so two threads showing the same
        // key cannot both add a message.
        val entry = synchronized(lock) {
            val existing = key?.let { find(it) }

            if (existing != null) {
                reportReplaced = existing.swap(content)

                existing
            } else {
                // A message shown without a key gets an object of its own, which equals nothing else.
                HollerEntry(
                    id = nextId++,
                    key = key ?: Any(),
                    owner = this,
                    content = content,
                ).also { queue.add(it) }
            }
        }

        reportReplaced?.invoke()

        return entry
    }

    /**
     * Finds the message that has the given [key] and has not been asked to close. A closing message
     * is left out, so its key is free again while it plays its exit animation. The caller holds [lock].
     *
     * @param key the key to look for.
     * @return the message, or `null` when there is none.
     */
    private fun find(key: Any): HollerEntry? = entries.firstOrNull {
        it.key == key && !it.isDismissed
    }
}
