// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.welcome

import app.campfire.auth.ui.login.LoginUiState
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

class WelcomeUiState(
  val loginUiState: LoginUiState,
  val eventSink: (WelcomeUiEvent) -> Unit,
) : CircuitUiState

sealed interface WelcomeUiEvent : CircuitUiEvent {
  data object AddCampsite : WelcomeUiEvent
}
