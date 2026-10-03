// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.di

import app.campfire.account.api.di.UserGraphManager
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import app.campfire.settings.api.UserSettingsStores
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class UserComponentManager(
  private val userComponentFactory: UserComponent.Factory,
  private val userSettingsStores: UserSettingsStores,
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
) : UserGraphManager {

  private val coroutineExceptionHandler = CoroutineExceptionHandler { context, throwable ->
    bark(LogPriority.ERROR, throwable = throwable) { "Coroutine Exception in UserComponentManager" }
  }

  override suspend fun create(userSession: UserSession) {
    // The user graph reads the account's settings synchronously, so have them in memory first
    userSettingsStores.load(userSession.userId)

    val newUserComponent = Trace.trace(DiTraceSections.USER_GRAPH) {
      userComponentFactory.create(userSession)
    }
    ComponentHolder.updateComponent(applicationScope, newUserComponent)

    newUserComponent.scopedDependencies.value.forEach { scoped ->
      applicationScope.launch(coroutineExceptionHandler) {
        bark { "Initializing: $scoped" }
        scoped.onCreate()
      }
    }
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
