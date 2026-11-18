package io.github.mradzinski.holler.sample

import io.github.mradzinski.holler.sample.core.input.DeviceInput
import io.github.mradzinski.holler.sample.core.network.NetworkMonitor
import io.github.mradzinski.holler.sample.core.network.PathNetworkMonitor
import io.github.mradzinski.holler.sample.di.startSampleKoin
import org.koin.dsl.module

/** Starts the sample's dependency injection on iOS. The Swift app calls it once, when it starts. */
fun startSample() {
    startSampleKoin(
        platformModule = module {
            single<NetworkMonitor> { PathNetworkMonitor() }
            single { DeviceInput(hasTouchScreen = true) }
        },
    )
}
