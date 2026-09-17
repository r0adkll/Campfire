// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.whatsnew

import app.campfire.core.app.ApplicationInfo
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.currentPlatform
import app.campfire.core.di.AppScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.settings.api.CampfireSettings
import app.campfire.whatsnew.api.Changelog
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
    return Changelog(loadFromDisk().forPlatform(currentPlatform))
  }

  override fun observeShouldShowWhatsNew(): Flow<Boolean> {
    return settings.observeLastSeenVersion()
      .map { lastSeenVersion ->
        lastSeenVersion != applicationInfo.versionName &&
          loadFromDisk().hasChangesFor(applicationInfo.versionName, currentPlatform)
      }
  }

  override suspend fun dismissWhatsNew() {
    settings.lastSeenVersion = applicationInfo.versionName
  }

  private suspend fun loadFromDisk(): List<VersionEntry> = withContext(dispatcherProvider.io) {
    try {
      val json = Json { isLenient = true }
      val changelogBytes = Res.readBytes("files/changelog.json")
      json.decodeFromString(changelogBytes.decodeToString())
    } catch (e: Exception) {
      bark(LogPriority.ERROR, throwable = e) { "Unable to read changelog from disk" }
      emptyList()
    }
  }
}
