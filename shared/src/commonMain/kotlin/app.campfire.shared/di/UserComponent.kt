package app.campfire.shared.di

import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.HomeScreen
import app.campfire.common.screens.WelcomeScreen
import app.campfire.core.di.ActivityScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.RootScreen
import app.campfire.core.session.UserSession
import app.campfire.shared.root.DrawerContent
import com.r0adkll.kimchi.annotations.ContributesSubcomponent
import com.slack.circuit.foundation.Circuit
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

  val drawerContent: DrawerContent

  @Provides @RootScreen
  @SingleIn(UserScope::class)
  fun provideRootScreen(userSession: UserSession): BaseScreen {
    return when (userSession) {
      is UserSession.LoggedIn -> HomeScreen
      UserSession.LoggedOut -> WelcomeScreen
    }
  }

  @ContributesSubcomponent.Factory
  interface Factory {
    fun create(userSession: UserSession): UserComponent
  }
}
