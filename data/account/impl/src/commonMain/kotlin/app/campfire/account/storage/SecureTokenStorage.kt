package app.campfire.account.storage

import app.campfire.account.api.AbsToken
import app.campfire.account.settings.TokenSettings
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.model.UserId
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.toSuspendSettings
import me.tatarka.inject.annotations.Inject

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

  override suspend fun put(userId: UserId, token: AbsToken) {
    settings.putString(
      key = accessTokenStorageKey(userId),
      value = token.accessToken,
    )
    token.refreshToken?.let {
      settings.putString(
        key = refreshTokenStorageKey(userId),
        value = it,
      )
    } ?: settings.remove(refreshTokenStorageKey(userId))
  }

  override suspend fun remove(userId: UserId) {
    settings.remove(accessTokenStorageKey(userId))
    settings.remove(refreshTokenStorageKey(userId))
  }

  override suspend fun getLegacy(userId: UserId): String? {
    return settings.getStringOrNull(legacyTokenStorageKey(userId))
  }

  override suspend fun removeLegacy(userId: UserId) {
    settings.remove(legacyTokenStorageKey(userId))
  }

  private fun accessTokenStorageKey(userId: String): String {
    return "accessToken_$userId"
  }

  private fun refreshTokenStorageKey(userId: String): String {
    return "refreshToken_$userId"
  }

  // TODO: Cleanup legacy token migration in future release (v0.11 maybe?)
  private fun legacyTokenStorageKey(userId: String): String {
    return "token_$userId"
  }
}
