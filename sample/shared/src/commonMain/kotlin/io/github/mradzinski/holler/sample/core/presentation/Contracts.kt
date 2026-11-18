package io.github.mradzinski.holler.sample.core.presentation

/** Marks the immutable state of one screen: everything the screen draws comes from it. */
internal interface UiState

/** Marks everything a screen can send to its view model. Each screen has its own sealed set. */
internal interface UiEvent
