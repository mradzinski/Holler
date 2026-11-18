package io.github.mradzinski.holler.sample.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Android's [NetworkMonitor]: online while `ConnectivityManager` has at least one network that reaches
 * the internet. It needs the `ACCESS_NETWORK_STATE` permission, declared in the app's manifest.
 *
 * @param context used to reach the system's `ConnectivityManager`.
 */
internal class ConnectivityNetworkMonitor(private val context: Context) : NetworkMonitor {

    /** Emits whether the device is online, starting with the current value. */
    override val isOnline: Flow<Boolean> = callbackFlow {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val networks = mutableSetOf<Network>()

        val callback = object : ConnectivityManager.NetworkCallback() {
            /**
             * Called by the system when a network that reaches the internet appears.
             *
             * @param network the network that appeared.
             */
            override fun onAvailable(network: Network) {
                networks += network
                trySend(true)
            }

            /**
             * Called by the system when a network goes away. The device is offline once none is left.
             *
             * @param network the network that went away.
             */
            override fun onLost(network: Network) {
                networks -= network
                trySend(networks.isNotEmpty())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        manager.registerNetworkCallback(request, callback)
        trySend(manager.activeNetwork != null)

        awaitClose { manager.unregisterNetworkCallback(callback) }
    }
        .distinctUntilChanged()
        .conflate()
}
