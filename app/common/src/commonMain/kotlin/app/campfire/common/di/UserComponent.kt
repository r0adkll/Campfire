package app.campfire.common.di

import app.campfire.auth.api.screen.AnalyticConsentScreen
import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.HomeScreen
import app.campfire.common.screens.WelcomeScreen
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.AppScope
import app.campfire.core.di.Scoped
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.di.qualifier.RootScreen
import app.campfire.core.session.UserSession
import app.campfire.sessions.api.SessionsRepository
import com.r0adkll.kimchi.annotations.ContributesSubcomponent
import com.slack.circuit.foundation.Circuit
import com.slack.circuitx.navigation.intercepting.NavigationEventListener
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.tatarka.inject.annotations.Provides

@SingleIn(UserScope::class)
@ContributesSubcomponent(
  scope = UserScope::class,
  parentScope = AppScope::class,
)
interface UserComponent {
  val scopedDependencies: Lazy<Set<Scoped>>

  val currentUserSession: UserSession

  // Expose the circuit information for UiScope
  val circuit: Circuit
  val navigationEventListeners: ImmutableList<NavigationEventListener>

  @get:RootScreen
  val rootScreen: BaseScreen

  @get:ForScope(UserScope::class)
  val coroutineScopeHolder: CoroutineScopeHolder

  val sessionsRepository: SessionsRepository

  @Provides @RootScreen
  @SingleIn(UserScope::class)
  fun provideRootScreen(userSession: UserSession): BaseScreen {
    return when (userSession) {
      is UserSession.LoggedIn -> if (userSession.showAnalyticsConsent) AnalyticConsentScreen else HomeScreen
      else -> WelcomeScreen
    }
  }

  @Provides
  @ForScope(UserScope::class)
  @SingleIn(UserScope::class)
  fun createCoroutineScopeHolder(): CoroutineScopeHolder {
    return CoroutineScopeHolder {
      CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
  }

  @ContributesSubcomponent.Factory
  interface Factory {
    fun create(userSession: UserSession): UserComponent
  }
}
