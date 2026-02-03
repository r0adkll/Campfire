package app.campfire.whatsnew

import app.campfire.core.app.ApplicationInfo
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.settings.api.CampfireSettings
import app.campfire.whatsnew.api.Changelog
import app.campfire.whatsnew.api.VersionChanges
import app.campfire.whatsnew.api.WhatsNewRepository
import campfire.infra.whats_new.impl.generated.resources.Res
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class WhatsNewRepositoryImpl(
  private val applicationInfo: ApplicationInfo,
  private val settings: CampfireSettings,
  private val dispatcherProvider: DispatcherProvider,
) : WhatsNewRepository {

  override suspend fun getChangelog(): Changelog {
    return loadFromDisk()
  }

  override fun observeShouldShowWhatsNew(): Flow<Boolean> {
    return settings.observeLastSeenVersion()
      .map { lastSeenVersion ->
        lastSeenVersion != applicationInfo.versionName
      }
  }

  override suspend fun dismissWhatsNew() {
    settings.lastSeenVersion = applicationInfo.versionName
  }

  private suspend fun loadFromDisk(): Changelog = withContext(dispatcherProvider.io) {
    try {
      val json = Json { isLenient = true }
      val changelogBytes = Res.readBytes("files/changelog.json")
      val changes: List<VersionChanges> = json.decodeFromString(changelogBytes.decodeToString())
      Changelog(changes)
    } catch (e: Exception) {
      bark(LogPriority.ERROR, throwable = e) { "Unable to read changelog from disk" }
      Changelog(emptyList())
    }
  }
}
