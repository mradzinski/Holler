package io.github.mradzinski.holler.sample.ui.email

import io.github.mradzinski.holler.sample.core.presentation.UiState
import io.github.mradzinski.holler.sample.ui.email.model.EmailUiModel

/**
 * The state of the email screen.
 *
 * @property email the open email; `null` until it is loaded.
 */
internal data class EmailUiState(val email: EmailUiModel? = null) : UiState
