package app.campfire.auth.ui.consent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.campfire.account.api.UserSessionManager
import app.campfire.auth.api.screen.AnalyticConsentScreen
import app.campfire.common.screens.HomeScreen
import app.campfire.core.di.UserScope
import app.campfire.core.session.UserSession
import app.campfire.settings.api.CampfireSettings
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(AnalyticConsentScreen::class, UserScope::class)
@Inject
class AnalyticConsentPresenter(
  @Assisted private val screen: AnalyticConsentScreen,
  @Assisted private val navigator: Navigator,
  private val userSession: UserSession,
  private val settings: CampfireSettings,
  private val userSessionManager: UserSessionManager,
) : Presenter<AnalyticConsentUiState> {

  @Composable
  override fun present(): AnalyticConsentUiState {
    var crashReportingEnabled by remember { mutableStateOf(settings.crashReportingEnabled) }
    var analyticReportingEnabled by remember { mutableStateOf(settings.analyticReportingEnabled) }

    return AnalyticConsentUiState(
      crashReportingEnabled = crashReportingEnabled,
      analyticReportingEnabled = analyticReportingEnabled,
    ) { event ->
      when (event) {
        is AnalyticConsentUiEvent.CrashReporting -> {
          crashReportingEnabled = event.enabled
        }
        is AnalyticConsentUiEvent.AnalyticReporting -> {
          analyticReportingEnabled = event.enabled
        }
        is AnalyticConsentUiEvent.ApplyConsent -> {
          settings.hasEverConsented = true
          settings.crashReportingEnabled = crashReportingEnabled
          settings.analyticReportingEnabled = analyticReportingEnabled
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
