package io.github.mradzinski.holler.sample.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Desktop's [NetworkMonitor]. The JVM has no API that reports connectivity changes, so it always says
 * online; the "Simulate offline" setting is how the offline behavior is tried on a desktop.
 */
internal class AlwaysOnlineNetworkMonitor : NetworkMonitor {

    /** Always `true`. */
    override val isOnline: Flow<Boolean> = flowOf(true)
}
