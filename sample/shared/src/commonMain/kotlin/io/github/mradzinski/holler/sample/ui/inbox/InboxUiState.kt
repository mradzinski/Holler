package io.github.mradzinski.holler.sample.ui.inbox

import io.github.mradzinski.holler.sample.core.presentation.UiState
import io.github.mradzinski.holler.sample.ui.inbox.model.EmailItemUiModel

/**
 * The state of the inbox screen.
 *
 * @property emails the rows of the inbox, newest first.
 * @property isRefreshing whether the app is checking for new emails.
 * @property showRefreshButton whether the screen has a button to check for new emails. It does where the
 * list cannot be pulled down, because the device has no touch screen.
 */
internal data class InboxUiState(
    val emails: List<EmailItemUiModel> = emptyList(),
    val isRefreshing: Boolean = false,
    val showRefreshButton: Boolean = false,
) : UiState
