package io.github.mradzinski.holler.sample.data.connectivity

import io.github.mradzinski.holler.sample.core.network.NetworkMonitor
import io.github.mradzinski.holler.sample.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** Whether the app can reach the network, as the rest of the app sees it. */
internal interface Connectivity {

    /** Emits `true` while the app is online and `false` while it is not, starting with the current value. */
    val isOnline: Flow<Boolean>
}

/**
 * Online when the platform's [NetworkMonitor] says so, unless the "Simulate offline" setting is on.
 * The setting makes the offline behavior easy to try on an emulator or a desktop.
 *
 * @param monitor the platform's network monitor.
 * @param settings where the "Simulate offline" setting is read from.
 */
internal class MonitoredConnectivity(monitor: NetworkMonitor, settings: SettingsRepository) : Connectivity {

    /** Emits whether the app is online: the platform says so and "Simulate offline" is off. */
    override val isOnline: Flow<Boolean> =
        combine(monitor.isOnline, settings.settings) { online, current -> online && !current.simulateOffline }
            .distinctUntilChanged()
}
