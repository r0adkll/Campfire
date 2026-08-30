// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.hardcover.auth

import app.campfire.bookinfo.api.ProviderLinkState
import app.campfire.bookinfo.hardcover.di.HardcoverSettings
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.model.UserId
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.toSuspendSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

/**
 * Encrypted storage for Hardcover personal access tokens, keyed by the ABS
 * user they were linked under. Lives in AppScope (mirroring the ABS token
 * storage) so links survive user-scope teardowns; the per-user view is
 * resolved by callers passing the current [UserId].
 */
@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@Inject
class HardcoverTokenStorage(
  @HardcoverSettings private val hardcoverSettings: Settings,
  private val dispatcherProvider: DispatcherProvider,
) {

  private val settings by lazy {
    hardcoverSettings.toSuspendSettings(dispatcherProvider.io)
  }

  private val changes = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }

  fun observeLinkState(userId: UserId): Flow<ProviderLinkState> = changes.map { readLinkState(userId) }

  suspend fun getToken(userId: UserId): String? {
    return settings.getStringOrNull(tokenKey(userId))
  }

  suspend fun link(userId: UserId, token: String, accountName: String?) {
    settings.putString(tokenKey(userId), token)
    accountName?.let { settings.putString(accountNameKey(userId), it) }
      ?: settings.remove(accountNameKey(userId))
    settings.remove(invalidKey(userId))
    changes.tryEmit(Unit)
  }

  suspend fun unlink(userId: UserId) {
    settings.remove(tokenKey(userId))
    settings.remove(accountNameKey(userId))
    settings.remove(invalidKey(userId))
    changes.tryEmit(Unit)
  }

  /** Records that the stored token was rejected by Hardcover, without deleting it. */
  suspend fun markInvalid(userId: UserId) {
    if (settings.getStringOrNull(tokenKey(userId)) == null) return
    settings.putBoolean(invalidKey(userId), true)
    changes.tryEmit(Unit)
  }

  private suspend fun readLinkState(userId: UserId): ProviderLinkState = when {
    settings.getStringOrNull(tokenKey(userId)) == null -> ProviderLinkState.NotLinked
    settings.getBoolean(invalidKey(userId), false) -> ProviderLinkState.Invalid
    else -> ProviderLinkState.Linked(settings.getStringOrNull(accountNameKey(userId)))
  }

  private fun tokenKey(userId: UserId) = "hardcoverToken_$userId"
  private fun accountNameKey(userId: UserId) = "hardcoverAccount_$userId"
  private fun invalidKey(userId: UserId) = "hardcoverTokenInvalid_$userId"
}
