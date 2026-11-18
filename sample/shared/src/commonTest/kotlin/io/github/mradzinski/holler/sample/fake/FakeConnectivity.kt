package io.github.mradzinski.holler.sample.fake

import io.github.mradzinski.holler.sample.data.connectivity.Connectivity
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A [Connectivity] a test switches by hand.
 *
 * @param online whether it starts online.
 */
internal class FakeConnectivity(online: Boolean = true) : Connectivity {

    /** Whether the app is online. A test sets its value to switch the connection. */
    override val isOnline: MutableStateFlow<Boolean> = MutableStateFlow(online)
}
