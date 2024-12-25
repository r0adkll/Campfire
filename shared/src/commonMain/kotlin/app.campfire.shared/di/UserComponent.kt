package app.campfire.shared.di

import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.HomeScreen
import app.campfire.common.screens.WelcomeScreen
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.ActivityScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.di.qualifier.RootScreen
import app.campfire.core.session.UserSession
import com.r0adkll.kimchi.annotations.ContributesSubcomponent
import com.slack.circuit.foundation.Circuit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.tatarka.inject.annotations.Provides

@SingleIn(UserScope::class)
@ContributesSubcomponent(
  scope = UserScope::class,
  parentScope = ActivityScope::class,
)
interface UserComponent {
  val currentUserSession: UserSession
  val circuit: Circuit

  @get:RootScreen
  val rootScreen: BaseScreen

  @get:ForScope(UserScope::class)
  val coroutineScopeHolder: CoroutineScopeHolder

  @Provides @RootScreen
  @SingleIn(UserScope::class)
  fun provideRootScreen(userSession: UserSession): BaseScreen {
    return when (userSession) {
      is UserSession.LoggedIn -> HomeScreen
      UserSession.LoggedOut -> WelcomeScreen
    }
  }

  @Provides
  @ForScope(UserScope::class)
  @SingleIn(UserScope::class)
  fun createCoroutineScopeHolder(): CoroutineScopeHolder {
    return CoroutineScopeHolder {
      CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
  }

  @ContributesSubcomponent.Factory
  interface Factory {
    fun create(userSession: UserSession): UserComponent
  }
}
