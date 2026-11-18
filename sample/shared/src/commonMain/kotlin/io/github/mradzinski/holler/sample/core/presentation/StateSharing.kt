package io.github.mradzinski.holler.sample.core.presentation

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed

/** How long a view model keeps computing its state after the interface stops watching it. */
private val StopTimeout: Duration = 5.seconds

/**
 * How every view model of the sample shares its state: computed while a screen is watching, and for a
 * few seconds more, so a rotation or a quick trip to another app does not restart the work.
 */
internal val WhileScreenIsWatching: SharingStarted = SharingStarted.WhileSubscribed(stopTimeout = StopTimeout)
