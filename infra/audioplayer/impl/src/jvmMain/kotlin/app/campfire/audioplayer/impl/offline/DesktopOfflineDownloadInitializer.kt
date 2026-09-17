// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

/**
 * Resumes downloads a previous run left unfinished. Unlike Android, desktop has no foreground
 * service restrictions to wait out, so this runs at startup.
 */
@ContributesMultibinding(AppScope::class)
@Inject
class DesktopOfflineDownloadInitializer(
  private val offlineDownloadManager: OfflineDownloadManager,
) : AppInitializer {
  override val priority: Int = AppInitializer.LOWEST_PRIORITY

  override suspend fun onInitialize() {
    offlineDownloadManager.resumeDownloads()
  }
}
