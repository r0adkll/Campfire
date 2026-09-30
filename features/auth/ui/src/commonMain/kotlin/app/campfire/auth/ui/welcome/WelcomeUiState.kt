// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.welcome

import app.campfire.account.api.RestorableAccount
import app.campfire.auth.ui.login.LoginUiState
import app.campfire.core.model.UserId
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

class WelcomeUiState(
  val loginUiState: LoginUiState,
  /**
   * Restored accounts being signed back in without a password
   */
  val restoringUserIds: Set<UserId> = emptySet(),
  val eventSink: (WelcomeUiEvent) -> Unit,
) : CircuitUiState

sealed interface WelcomeUiEvent : CircuitUiEvent {
  data object AddCampsite : WelcomeUiEvent
  data class RestoreAccount(val account: RestorableAccount) : WelcomeUiEvent
}
