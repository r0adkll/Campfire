// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.storage

import app.campfire.account.api.AbsToken
import app.campfire.account.settings.TokenSettings
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.model.UserId
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.toSuspendSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class SecureTokenStorage(
  @TokenSettings private val tokenSettings: Settings,
  private val dispatcherProvider: DispatcherProvider,
) : TokenStorage {

  @OptIn(ExperimentalSettingsApi::class)
  private val settings by lazy {
    tokenSettings.toSuspendSettings(dispatcherProvider.io)
  }

  override suspend fun get(userId: UserId): AbsToken? {
    val accessToken = settings.getStringOrNull(accessTokenStorageKey(userId)) ?: return null
    val refreshToken = settings.getStringOrNull(refreshTokenStorageKey(userId))
    return AbsToken(accessToken, refreshToken)
  }

  override suspend fun has(userId: UserId): Boolean {
    return settings.hasKey(accessTokenStorageKey(userId))
  }

  override suspend fun put(userId: UserId, token: AbsToken) {
    // The refresh token goes first. The two writes aren't atomic, and a rotation has already spent
    // the old refresh token on the server: if the app dies between them, a new refresh token with
    // an old access token recovers on the next refresh, while the reverse can never refresh again.
    token.refreshToken?.let {
      settings.putString(
        key = refreshTokenStorageKey(userId),
        value = it,
      )
    } ?: settings.remove(refreshTokenStorageKey(userId))
    settings.putString(
      key = accessTokenStorageKey(userId),
      value = token.accessToken,
    )
  }

  override suspend fun remove(userId: UserId) {
    settings.remove(accessTokenStorageKey(userId))
    settings.remove(refreshTokenStorageKey(userId))
  }

  private fun accessTokenStorageKey(userId: String): String {
    return "accessToken_$userId"
  }

  private fun refreshTokenStorageKey(userId: String): String {
    return "refreshToken_$userId"
  }
}
