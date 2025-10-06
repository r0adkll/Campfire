package app.campfire.common.di

import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.rootScreen
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
  val circuit: Circuit

  @get:RootScreen
  val rootScreen: BaseScreen

  @get:ForScope(UserScope::class)
  val coroutineScopeHolder: CoroutineScopeHolder

  val sessionsRepository: SessionsRepository

  @Provides @RootScreen
  @SingleIn(UserScope::class)
  fun provideRootScreen(userSession: UserSession): BaseScreen {
    return userSession.rootScreen
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
