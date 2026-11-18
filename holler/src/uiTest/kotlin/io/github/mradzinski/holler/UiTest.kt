package io.github.mradzinski.holler

/**
 * The base of every test class that draws a message. It is empty everywhere but on Android, where it
 * carries the test runner that gives a test Android's own code to draw with. A test class only has to
 * extend it.
 */
expect abstract class UiTest()
