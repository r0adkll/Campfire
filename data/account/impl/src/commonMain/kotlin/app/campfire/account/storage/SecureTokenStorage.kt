package app.campfire.account.storage

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

  override suspend fun get(userId: UserId): String? {
    return settings.getStringOrNull(tokenStorageKey(userId))
  }

  override suspend fun put(userId: UserId, token: String) {
    settings.putString(
      key = tokenStorageKey(userId),
      value = token,
    )
  }

  override suspend fun remove(userId: UserId) {
    settings.remove(tokenStorageKey(userId))
  }

  private fun tokenStorageKey(userId: String): String {
    return "token_$userId"
  }
}
