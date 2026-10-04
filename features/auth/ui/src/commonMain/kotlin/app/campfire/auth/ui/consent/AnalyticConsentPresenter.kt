// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.consent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.campfire.account.api.UserSessionManager
import app.campfire.auth.api.screen.AnalyticConsentScreen
import app.campfire.common.screens.HomeScreen
import app.campfire.core.di.UserScope
import app.campfire.core.session.UserSession
import app.campfire.settings.api.PrivacySettings
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject

@CircuitInject(AnalyticConsentScreen::class, UserScope::class)
@Inject
class AnalyticConsentPresenter(
  private val screen: AnalyticConsentScreen,
  private val navigator: Navigator,
  private val userSession: UserSession,
  private val privacySettings: PrivacySettings,
  private val userSessionManager: UserSessionManager,
) : Presenter<AnalyticConsentUiState> {

  @Composable
  override fun present(): AnalyticConsentUiState {
    // Start from the stored choices; the user's toggles here override them until applied
    val storedCrashReporting by remember { privacySettings.observeCrashReportingEnabled() }
      .collectAsState(PrivacySettings.DEFAULT_CRASH_REPORTING_ENABLED)
    val storedAnalyticReporting by remember { privacySettings.observeAnalyticReportingEnabled() }
      .collectAsState(PrivacySettings.DEFAULT_ANALYTIC_REPORTING_ENABLED)
    var crashReportingChoice by remember { mutableStateOf<Boolean?>(null) }
    var analyticReportingChoice by remember { mutableStateOf<Boolean?>(null) }
    val crashReportingEnabled = crashReportingChoice ?: storedCrashReporting
    val analyticReportingEnabled = analyticReportingChoice ?: storedAnalyticReporting

    return AnalyticConsentUiState(
      crashReportingEnabled = crashReportingEnabled,
      analyticReportingEnabled = analyticReportingEnabled,
    ) { event ->
      when (event) {
        is AnalyticConsentUiEvent.CrashReporting -> {
          crashReportingChoice = event.enabled
        }
        is AnalyticConsentUiEvent.AnalyticReporting -> {
          analyticReportingChoice = event.enabled
        }
        is AnalyticConsentUiEvent.ApplyConsent -> {
          privacySettings.setHasEverConsented(true)
          privacySettings.setCrashReportingEnabled(crashReportingEnabled)
          privacySettings.setAnalyticReportingEnabled(analyticReportingEnabled)
          if (userSession is UserSession.LoggedIn) {
            navigator.resetRoot(HomeScreen)
          } else {
            error("We shouldn't be showing data collection consent for non-logged in user sessions.")
          }
        }
      }
    }
  }
}
