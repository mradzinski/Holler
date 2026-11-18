package io.github.mradzinski.holler.sample

import io.github.mradzinski.holler.sample.core.network.BrowserNetworkMonitor
import io.github.mradzinski.holler.sample.core.network.NetworkMonitor
import io.github.mradzinski.holler.sample.di.startSampleKoin
import org.koin.dsl.module

/** Starts the sample's dependency injection on the web. Call it once, before drawing the page. */
fun startSample() {
    startSampleKoin(platformModule = module { single<NetworkMonitor> { BrowserNetworkMonitor() } })
}
