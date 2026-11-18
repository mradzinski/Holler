package io.github.mradzinski.holler.sample

import android.content.Context
import io.github.mradzinski.holler.sample.core.input.DeviceInput
import io.github.mradzinski.holler.sample.core.network.ConnectivityNetworkMonitor
import io.github.mradzinski.holler.sample.core.network.NetworkMonitor
import io.github.mradzinski.holler.sample.di.startSampleKoin
import org.koin.dsl.module

/**
 * Starts the sample's dependency injection on Android. Call it once, from the `Application`.
 *
 * @param context the application context, which the network monitor keeps.
 */
fun startSample(context: Context) {
    val appContext = context.applicationContext

    startSampleKoin(
        platformModule = module {
            single<NetworkMonitor> { ConnectivityNetworkMonitor(appContext) }
            single { DeviceInput(hasTouchScreen = true) }
        },
    )
}
