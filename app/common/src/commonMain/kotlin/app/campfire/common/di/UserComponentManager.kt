// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.di

import app.campfire.account.api.di.UserGraphManager
import app.campfire.auth.api.screen.AnalyticConsentScreen
import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.HomeScreen
import app.campfire.common.screens.LoginScreen
import app.campfire.common.screens.WelcomeScreen
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.session.UserSession
import app.campfire.settings.api.PrivacySettings
import app.campfire.tracing.DiTraceSections
import app.campfire.tracing.Trace
import app.campfire.tracing.trace
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class UserComponentManager(
  private val userComponentFactory: UserComponent.Factory,
  private val privacySettings: PrivacySettings,
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
) : UserGraphManager {

  private val coroutineExceptionHandler = CoroutineExceptionHandler { context, throwable ->
    bark(LogPriority.ERROR, throwable = throwable) { "Coroutine Exception in UserComponentManager" }
  }

  override suspend fun create(userSession: UserSession) {
    val rootScreen = rootScreenFor(userSession)
    val newUserComponent = Trace.trace(DiTraceSections.USER_GRAPH) {
      userComponentFactory.create(userSession, rootScreen)
    }
    ComponentHolder.updateComponent(applicationScope, newUserComponent)

    newUserComponent.scopedDependencies.value.forEach { scoped ->
      applicationScope.launch(coroutineExceptionHandler) {
        bark { "Initializing: $scoped" }
        scoped.onCreate()
      }
    }
  }

  private suspend fun rootScreenFor(userSession: UserSession): BaseScreen = when (userSession) {
    is UserSession.NeedsAuthentication -> LoginScreen.ReAuthentication(userSession.server)
    is UserSession.LoggedIn -> {
      if (privacySettings.observeHasEverConsented().first()) HomeScreen else AnalyticConsentScreen
    }
    else -> WelcomeScreen
  }

  override suspend fun destroy() {
    val userComponent = ComponentHolder.component<UserComponent>()

    withContext(applicationScope.coroutineContext + coroutineExceptionHandler) {
      userComponent.scopedDependencies.value
        .map { scoped ->
          bark { "Destroying: $scoped" }
          async {
            withTimeoutOrNull(150L) {
              scoped.onDestroy()
            }
          }
        }
        .awaitAll()
    }

    // Cancel the scope!
    bark { "Tearing down UserScope coroutine scope" }
    userComponent.coroutineScopeHolder.cancel("UserScope destroyed")
  }
}
