// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.scan

import androidx.compose.runtime.Stable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

/**
 * @property sawOtherCode the camera recently read a QR code that isn't a setup link
 */
@Stable
data class ScanSetupCodeUiState(
  val sawOtherCode: Boolean,
  val eventSink: (ScanSetupCodeUiEvent) -> Unit,
) : CircuitUiState

sealed interface ScanSetupCodeUiEvent : CircuitUiEvent {

  /** The camera read a QR code holding [text]. */
  class Scanned(val text: String) : ScanSetupCodeUiEvent {
    // A setup code carries a service token; keep it out of anything that logs events
    override fun toString(): String = "Scanned(text=<redacted>)"
  }

  data object Close : ScanSetupCodeUiEvent
}
