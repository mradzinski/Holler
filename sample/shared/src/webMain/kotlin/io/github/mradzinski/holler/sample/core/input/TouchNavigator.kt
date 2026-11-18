package io.github.mradzinski.holler.sample.core.input

/** The part of the browser's `navigator` object that tells whether the device has a touch screen. */
internal external interface TouchNavigator {
    /** How many fingers the screen can follow at once; `0` when it is not a touch screen. */
    val maxTouchPoints: Int
}

/** The browser's global `navigator` object. */
internal external val navigator: TouchNavigator

/**
 * The web's [DeviceInput], for both JS and Wasm. A page can be open on a phone or on a computer, so it
 * asks the browser.
 *
 * @return the input of the device the page is open on.
 */
internal fun browserDeviceInput(): DeviceInput = DeviceInput(hasTouchScreen = navigator.maxTouchPoints > 0)
