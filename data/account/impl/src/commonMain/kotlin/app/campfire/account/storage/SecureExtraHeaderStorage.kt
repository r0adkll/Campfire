package app.campfire.account.storage

import app.campfire.account.settings.ExtraHeaderSettings
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
class SecureExtraHeaderStorage(
  @ExtraHeaderSettings private val extraHeaderSettings: Settings,
  private val dispatcherProvider: DispatcherProvider,
) : ExtraHeaderStorage {

  @OptIn(ExperimentalSettingsApi::class)
  private val settings by lazy {
    extraHeaderSettings.toSuspendSettings(dispatcherProvider.io)
  }

  override suspend fun get(userId: UserId): Map<String, String>? {
    val rawExtraHeaders = settings.getStringOrNull(storageKey(userId))
    return rawExtraHeaders?.let { raw -> deserialize(raw) }
  }

  override suspend fun put(
    userId: UserId,
    headers: Map<String, String>,
  ) {
    settings.putString(storageKey(userId), serialize(headers))
  }

  override suspend fun remove(userId: UserId) {
    settings.remove(storageKey(userId))
  }

  private fun serialize(extraHeaders: Map<String, String>): String {
    return extraHeaders
      .map { (name, value) -> "${name}$PAIR_SEPARATOR$value" }
      .joinToString(SET_SEPARATOR)
  }

  private fun deserialize(raw: String): Map<String, String> {
    val sets = raw.split(SET_SEPARATOR)
    return sets.mapNotNull { set ->
      val pair = set.split(PAIR_SEPARATOR)
      if (pair.size == 2) {
        pair[0] to pair[1]
      } else {
        null
      }
    }.toMap()
  }

  private fun storageKey(userId: UserId): String {
    return "extraHeaders_$userId"
  }

  companion object {
    private const val SET_SEPARATOR = ";|;"
    private const val PAIR_SEPARATOR = ":|:"
  }
}
