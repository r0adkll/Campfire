// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/**
 * Resumes downloads a previous run left unfinished. Unlike Android, desktop has no foreground
 * service restrictions to wait out, so this runs at startup.
 */
@ContributesIntoSet(AppScope::class)
@Inject
class DesktopOfflineDownloadInitializer(
  private val offlineDownloadManager: OfflineDownloadManager,
) : AppInitializer {
  override val priority: Int = AppInitializer.LOWEST_PRIORITY

  override suspend fun onInitialize() {
    offlineDownloadManager.resumeDownloads()
  }
}
