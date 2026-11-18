package io.github.mradzinski.holler.sample

import io.github.mradzinski.holler.sample.core.network.AlwaysOnlineNetworkMonitor
import io.github.mradzinski.holler.sample.core.network.NetworkMonitor
import io.github.mradzinski.holler.sample.di.startSampleKoin
import org.koin.dsl.module

/** Starts the sample's dependency injection on desktop. Call it once, before opening the window. */
fun startSample() {
    startSampleKoin(platformModule = module { single<NetworkMonitor> { AlwaysOnlineNetworkMonitor() } })
}
