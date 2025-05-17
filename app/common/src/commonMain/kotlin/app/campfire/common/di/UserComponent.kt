package app.campfire.common.di

import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.rootScreen
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.AppScope
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.RootScreen
import app.campfire.core.session.UserSession
import com.slack.circuit.foundation.Circuit
import dev.zacsweers.metro.ContributesGraphExtension
import dev.zacsweers.metro.ForScope
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@SingleIn(UserScope::class)
@ContributesGraphExtension(UserScope::class)
interface UserComponent : SessionRepositoryComponent {
  val currentUserSession: UserSession
  val circuit: Circuit

  @RootScreen
  val rootScreen: BaseScreen

  @ForScope(UserScope::class)
  val coroutineScopeHolder: CoroutineScopeHolder

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

  @ContributesGraphExtension.Factory(AppScope::class)
  interface Factory {
    fun create(@Provides userSession: UserSession): UserComponent
  }
}
