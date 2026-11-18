package io.github.mradzinski.holler

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The base of the tests that draw a message. On Android it runs them with Robolectric, which gives a
 * test Android's own code on the computer, with no emulator. The settings are in `robolectric.properties`.
 */
@RunWith(RobolectricTestRunner::class)
actual abstract class UiTest
