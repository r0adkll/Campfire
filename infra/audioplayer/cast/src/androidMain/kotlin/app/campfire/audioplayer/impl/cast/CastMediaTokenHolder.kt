// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.cast

import app.campfire.account.api.AccountManager
import app.campfire.account.api.UserSessionManager
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.session.userId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

/**
 * Synchronous snapshot of the current ABS access token for the cast path.
 *
 * The Cast receiver fetches media URLs itself, so authentication must ride in the URL — but
 * [CampfireMediaItemConverter] runs synchronously on the main thread while [AccountManager]'s
 * token accessor suspends. This holder bridges the two: [refresh] is fired at the moments a cast
 * flow begins (player construction, device connect), and the converter reads the latest snapshot.
 *
 * Best-effort by design: ABS access tokens expire (~1h) and this holder makes no attempt to keep
 * a long cast session alive past that — the dead-cast watchdog recovers, and the proper fix is
 * the planned `/api/items/{id}/play` session flow with credential-free public track URLs.
 */
@SingleIn(AppScope::class)
@Inject
class CastMediaTokenHolder(
  private val accountManager: AccountManager,
  private val userSessionManager: UserSessionManager,
  private val dispatcherProvider: DispatcherProvider,
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
) {

  @Volatile
  var accessToken: String? = null
    private set

  fun refresh() {
    applicationScope.launch(dispatcherProvider.io) {
      try {
        val userId = userSessionManager.current.userId ?: return@launch
        accessToken = accountManager.getToken(userId)?.accessToken
      } catch (e: Throwable) {
        bark(LogPriority.WARN, throwable = e) { "Unable to refresh cast media token" }
      }
    }
  }
}
