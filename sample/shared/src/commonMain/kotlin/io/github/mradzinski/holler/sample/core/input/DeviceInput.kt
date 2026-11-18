package io.github.mradzinski.holler.sample.core.input

/**
 * What the app knows about how the user points at the screen. Each platform binds its own: a phone
 * has a touch screen, a desktop window does not, and a browser says which it is.
 *
 * @property hasTouchScreen whether the user can touch the screen, and so make gestures such as pulling
 * a list down. Where they cannot, a gesture needs a button next to it.
 */
internal data class DeviceInput(val hasTouchScreen: Boolean)
