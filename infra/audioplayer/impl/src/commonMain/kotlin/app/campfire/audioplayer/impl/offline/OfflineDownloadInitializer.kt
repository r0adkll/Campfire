// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.core.app.AppInitializer

// TODO: This requires the Activity to be fore-grounded or otherwise it can
//  cause ForegroundService from Background errors on newer Android API versions.
//  We should bake in some sort of background/foreground state mechanisms into the
//  app initializer re-write. See (#471)
// @ContributesMultibinding(AppScope::class)
// @Inject
class OfflineDownloadInitializer(
  private val offlineDownloadManager: OfflineDownloadManager,
) : AppInitializer {
  override val priority: Int = AppInitializer.LOWEST_PRIORITY

  override suspend fun onInitialize() {
    /**
     * Make sure we resume any paused or incomplete media downloads
     */
    offlineDownloadManager.resumeDownloads()
  }
}
