// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.consent

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class AnalyticConsentUiState(
  val crashReportingEnabled: Boolean,
  val analyticReportingEnabled: Boolean,
  val eventSink: (AnalyticConsentUiEvent) -> Unit,
) : CircuitUiState

sealed interface AnalyticConsentUiEvent : CircuitUiEvent {
  data class CrashReporting(val enabled: Boolean) : AnalyticConsentUiEvent
  data class AnalyticReporting(val enabled: Boolean) : AnalyticConsentUiEvent
  data object ApplyConsent : AnalyticConsentUiEvent
}
