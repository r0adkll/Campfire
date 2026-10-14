// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.connect

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Stable
data class ConnectUiState(
  val content: ConnectContent,
  val eventSink: (ConnectUiEvent) -> Unit,
) : CircuitUiState

@Immutable
sealed interface ConnectContent {

  /** Checking whether there's already an account on the link's server. */
  data object Loading : ConnectContent

  /**
   * Ask before saving anything.
   *
   * @property serverHost where the service token will be sent
   * @property serverName the name the link gives the server, which anyone can make up
   * @property existingUserName the account on this server that gets the token, or `null` when the
   *   link adds a new campsite
   */
  data class Confirm(
    val serverHost: String,
    val serverName: String?,
    val existingUserName: String?,
    val signInWithOpenId: Boolean,
    val isSaving: Boolean = false,
  ) : ConnectContent

  /** An existing account on the server now uses the link's service token. */
  data class Updated(
    val serverHost: String,
    val userName: String,
  ) : ConnectContent

  /** The link comes from a newer Campfire. */
  data object UnsupportedVersion : ConnectContent

  data object Invalid : ConnectContent
}

sealed interface ConnectUiEvent : CircuitUiEvent {
  data object Confirm : ConnectUiEvent

  /** Cancel, or close once there's nothing left to do. Nothing from the link is kept. */
  data object Dismiss : ConnectUiEvent
}
