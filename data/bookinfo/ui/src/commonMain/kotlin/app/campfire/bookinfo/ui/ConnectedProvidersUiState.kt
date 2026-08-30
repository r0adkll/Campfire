// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.ui

import androidx.compose.runtime.Immutable
import app.campfire.bookinfo.api.ProviderCapabilities
import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderLinkState
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Immutable
data class ConnectedProvidersUiState(
  val providers: List<ProviderRowState>,
  val isClearingCache: Boolean = false,
  val eventSink: (ConnectedProvidersUiEvent) -> Unit,
) : CircuitUiState

@Immutable
data class ProviderRowState(
  val id: ProviderId,
  val name: String,
  val capabilities: ProviderCapabilities,
  val enabled: Boolean,
  val linkState: ProviderLinkState,
  val supportsLinking: Boolean,
  val linkHelpUrl: String?,
  val isVerifying: Boolean = false,
  val linkFailed: Boolean = false,
)

sealed interface ConnectedProvidersUiEvent : CircuitUiEvent {
  data object Back : ConnectedProvidersUiEvent
  data object ClearCache : ConnectedProvidersUiEvent
  data class ToggleEnabled(val id: ProviderId, val enabled: Boolean) : ConnectedProvidersUiEvent
  data class Link(val id: ProviderId, val token: String) : ConnectedProvidersUiEvent
  data class Unlink(val id: ProviderId) : ConnectedProvidersUiEvent
  data class OpenLinkHelp(val url: String) : ConnectedProvidersUiEvent
}
