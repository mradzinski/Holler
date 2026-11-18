package io.github.mradzinski.holler.sample.core.network

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_queue_create

/**
 * iOS's [NetworkMonitor]: online while `NWPathMonitor` reports a satisfied path. The monitor reports
 * the current path right after it starts, so the flow begins with the current value.
 */
internal class PathNetworkMonitor : NetworkMonitor {

    /** Emits whether the device is online, starting with the current value. */
    override val isOnline: Flow<Boolean> = callbackFlow {
        val monitor = nw_path_monitor_create()

        nw_path_monitor_set_queue(monitor, dispatch_queue_create(QUEUE_LABEL, null))
        nw_path_monitor_set_update_handler(monitor) { path ->
            trySend(nw_path_get_status(path) == nw_path_status_satisfied)
        }
        nw_path_monitor_start(monitor)

        awaitClose { nw_path_monitor_cancel(monitor) }
    }
        .distinctUntilChanged()
        .conflate()

    /** The constants of this class. */
    private companion object {
        /** Names the dispatch queue the monitor reports on, as it shows in a debugger. */
        const val QUEUE_LABEL: String = "io.github.mradzinski.holler.sample.network"
    }
}
