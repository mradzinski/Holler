package io.github.mradzinski.holler.sample.core.network

import kotlinx.coroutines.flow.Flow

/**
 * Tells whether the device has a usable network. There is one implementation per platform, each bound
 * in that platform's Koin module: `ConnectivityManager` on Android, `NWPathMonitor` on iOS and the
 * browser's `online` / `offline` events on the web. Desktop has no such API and always reports online.
 */
internal interface NetworkMonitor {

    /** Emits `true` while the device is online and `false` while it is not, starting with the current value. */
    val isOnline: Flow<Boolean>
}
