// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.di

import app.campfire.auth.api.screen.AnalyticConsentScreen
import app.campfire.common.root.automation.UserAutomationDeepLinks
import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.HomeScreen
import app.campfire.common.screens.LoginScreen
import app.campfire.common.screens.WelcomeScreen
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.AppScope
import app.campfire.core.di.Scoped
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.di.qualifier.RootScreen
import app.campfire.core.session.UserSession
import app.campfire.sessions.api.SessionsRepository
import app.campfire.settings.api.PrivacySettings
import com.slack.circuit.foundation.Circuit
import com.slack.circuitx.navigation.intercepting.NavigationEventListener
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@GraphExtension(UserScope::class)
interface UserComponent {
  val scopedDependencies: Lazy<Set<Scoped>>

  val currentUserSession: UserSession

  // Expose the circuit information for UiScope
  val circuit: Circuit
  val navigationEventListeners: ImmutableList<NavigationEventListener>

  @RootScreen
  val rootScreen: () -> BaseScreen

  @ForScope(UserScope::class)
  val coroutineScopeHolder: CoroutineScopeHolder

  val sessionsRepository: SessionsRepository

  /** Debug-only automation deep links that need user-scoped dependencies */
  val automationDeepLinks: UserAutomationDeepLinks

  @Provides @RootScreen
  fun provideRootScreen(
    userSession: UserSession,
    privacySettings: PrivacySettings,
  ): BaseScreen {
    return when (userSession) {
      is UserSession.NeedsAuthentication -> LoginScreen.ReAuthentication(userSession.server)
      is UserSession.LoggedIn -> if (!privacySettings.hasEverConsented) AnalyticConsentScreen else HomeScreen
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

  @ContributesTo(AppScope::class)
  @GraphExtension.Factory
  interface Factory {
    fun create(@Provides userSession: UserSession): UserComponent
  }
}
