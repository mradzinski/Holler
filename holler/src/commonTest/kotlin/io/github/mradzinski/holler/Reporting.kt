package io.github.mradzinski.holler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Waits for the message to close and adds the reason to [reasons], the way an app would wait in a
 * scope of its own. The wait runs unconfined, so it has started when this returns and the reason is
 * added the moment the message closes, with no dispatcher for a test to advance.
 *
 * @param reasons where the reason is added.
 * @return this handle, so the call can follow `show`.
 */
internal fun HollerHandle.reportTo(reasons: MutableList<HollerDismissReason>): HollerHandle {
    CoroutineScope(Dispatchers.Unconfined).launch { reasons += awaitDismiss() }

    return this
}
