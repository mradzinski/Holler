package io.github.mradzinski.holler.sample.core.network

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/** The part of the browser's `navigator` object this monitor reads. */
internal external interface BrowserNavigator {

    /** Whether the browser believes it is online. */
    val onLine: Boolean
}

/** The browser's global `navigator` object. */
internal external val navigator: BrowserNavigator

/**
 * The browser's global `addEventListener`, which listens on the window.
 *
 * @param type the name of the event.
 * @param callback called each time the event fires.
 */
internal external fun addEventListener(type: String, callback: () -> Unit)

/**
 * The browser's global `removeEventListener`.
 *
 * @param type the name of the event.
 * @param callback the listener to remove.
 */
internal external fun removeEventListener(type: String, callback: () -> Unit)

/**
 * The web's [NetworkMonitor], for both JS and Wasm: it starts from `navigator.onLine` and follows the
 * window's `online` and `offline` events.
 */
internal class BrowserNetworkMonitor : NetworkMonitor {

    /** Emits whether the device is online, starting with the current value. */
    override val isOnline: Flow<Boolean> = callbackFlow {
        val onOnline: () -> Unit = { trySend(true) }
        val onOffline: () -> Unit = { trySend(false) }

        addEventListener(ONLINE_EVENT, onOnline)
        addEventListener(OFFLINE_EVENT, onOffline)
        trySend(navigator.onLine)

        awaitClose {
            removeEventListener(ONLINE_EVENT, onOnline)
            removeEventListener(OFFLINE_EVENT, onOffline)
        }
    }
        .distinctUntilChanged()
        .conflate()

    /** The constants of this class. */
    private companion object {
        /** The window event fired when the browser gets its connection back. */
        const val ONLINE_EVENT: String = "online"

        /** The window event fired when the browser loses its connection. */
        const val OFFLINE_EVENT: String = "offline"
    }
}
