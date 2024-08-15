package app.campfire.account.storage

import app.campfire.account.settings.TokenSettings
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.toSuspendSettings
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
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

  override suspend fun get(serverUrl: String): String? {
    return settings.getStringOrNull(tokenStorageKey(serverUrl))
  }

  override suspend fun put(serverUrl: String, token: String) {
    settings.putString(
      key = tokenStorageKey(serverUrl),
      value = token,
    )
  }

  @OptIn(ExperimentalEncodingApi::class)
  private fun tokenStorageKey(serverUrl: String): String {
    val serverUrlBase64 = Base64.UrlSafe.encode(serverUrl.encodeToByteArray())
    return "token_${serverUrlBase64}"
  }
}
