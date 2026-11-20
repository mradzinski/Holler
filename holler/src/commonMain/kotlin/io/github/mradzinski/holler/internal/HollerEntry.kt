package io.github.mradzinski.holler.internal

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.mradzinski.holler.Holler
import io.github.mradzinski.holler.HollerDismissReason
import io.github.mradzinski.holler.HollerHandle
import kotlin.concurrent.Volatile
import kotlin.coroutines.resume
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * One message of a host: its place in the queue, its key and its current content.
 *
 * @param id unique number of the message within its host state, used as its composition key.
 * @param key the key of the message, the caller's or one made by the library.
 * @param owner the host state the message belongs to.
 * @param content what the message shows at first.
 */
internal class HollerEntry(
    val id: Long,
    override val key: Any,
    private val owner: Holler,
    content: HollerContent,
) : HollerHandle {

    /**
     * What the message shows now. Observable, so the host redraws the card when [swap] changes it.
     */
    var content: HollerContent by mutableStateOf(content)
        private set

    /** The app's own object when the message was shown as a value; `null` for a text message. It follows [content]. */
    override val value: Any?
        get() = content.value.takeUnless { it is TextMessage }

    /** Drives the enter and exit animation. Starts hidden so the first frame animates in. */
    val visibility: MutableTransitionState<Boolean> = MutableTransitionState(false)

    /**
     * Where the message is in its life: waiting, on screen, or on its way out. It is changed under the
     * owner's lock and may be read from any thread.
     */
    @Volatile
    private var stage: Stage = Stage.Queued

    /** `true` once the host has started drawing the message, i.e. it has left the queue. */
    val wasShown: Boolean get() = stage != Stage.Queued

    /** `true` once the message has been asked to close, so it is only closed once. */
    val isDismissed: Boolean get() = stage == Stage.Leaving

    /**
     * `true` once a card has been drawn for the message, which is when its enter animation starts. It is
     * how a card that takes the message over later knows the enter is not its own to play. Only touched
     * while composing, on the UI thread.
     */
    var enterStarted: Boolean = false

    /** Why the message closed, once it has. It is what a wait that starts afterwards returns. */
    @Volatile
    private var closedFor: HollerDismissReason? = null

    /**
     * Records that the host has started drawing the message and starts its enter animation.
     * Does nothing unless the message is still queued, so a message already closing stays closed.
     */
    fun markShown() {
        synchronized(owner.lock) {
            if (stage == Stage.Queued) {
                stage = Stage.Visible
                visibility.targetState = true
            }
        }
    }

    /**
     * Gives the message new content in the place it already has, on screen or in the queue. The caller
     * holds the owner's lock, and afterwards runs what this returns with the lock released.
     *
     * @param content what the message shows from now on.
     * @return resumes the callers that were waiting on the content the message had, with
     * [HollerDismissReason.Replaced]. It is returned, not run here, because resuming a caller can run the
     * app's code at once: it may show or close messages itself, and must not run inside the lock.
     */
    fun swap(content: HollerContent): () -> Unit {
        val waiting = this.content.takeWaiters()

        this.content = content

        return { waiting.forEach { it.resume(HollerDismissReason.Replaced) } }
    }

    /**
     * Closes the message for the given [reason]: either starts the exit animation or, if the message
     * never left the queue, removes it directly, then tells the callers that are waiting, once.
     *
     * @param reason why the message is closing.
     */
    fun dismiss(reason: HollerDismissReason) {
        // The stage changes inside the lock, so two threads closing the same message cannot both report
        // it. The waiting callers are resumed after it, because that can run the app's code at once.
        val waiting = synchronized(owner.lock) {
            val previous = stage

            if (previous == Stage.Leaving) {
                emptyList()
            } else {
                stage = Stage.Leaving
                closedFor = reason

                when (previous) {
                    Stage.Queued -> owner.remove(this)
                    Stage.Visible -> visibility.targetState = false
                    Stage.Leaving -> Unit
                }

                content.takeWaiters()
            }
        }

        waiting.forEach { it.resume(reason) }
    }

    /**
     * Suspends until the content the message shows now is gone, and says why. See
     * [HollerHandle.awaitDismiss] for the rules.
     *
     * @return why the message closed, or [HollerDismissReason.Replaced] if it was replaced.
     */
    override suspend fun awaitDismiss(): HollerDismissReason = suspendCancellableCoroutine { continuation ->
        var waitingOn: HollerContent? = null

        // Checking whether the message already closed and joining the waiting callers is one step, so a
        // close that happens at the same moment cannot be missed.
        val alreadyClosedFor = synchronized(owner.lock) {
            val reason = closedFor

            if (reason == null) {
                waitingOn = content.also { it.waiters += continuation }
            }

            reason
        }

        if (alreadyClosedFor != null) {
            continuation.resume(alreadyClosedFor)
        } else {
            // A caller that stops waiting, because its scope was cancelled, is forgotten at once: the
            // message keeps nothing of it.
            continuation.invokeOnCancellation {
                synchronized(owner.lock) { waitingOn?.waiters?.remove(continuation) }
            }
        }
    }

    /** Closes the message, reporting the matching [HollerDismissReason]. */
    override fun dismiss(): Unit = dismiss(HollerDismissReason.Programmatic)

    /** The stages a message goes through inside its host. It only ever moves forward. */
    private enum class Stage {
        /** Shown by the caller but not yet drawn: waiting for a free slot. */
        Queued,

        /** Being drawn by the host. */
        Visible,

        /** Asked to close: playing its exit animation, or already removed if it was still queued. */
        Leaving,
    }
}
