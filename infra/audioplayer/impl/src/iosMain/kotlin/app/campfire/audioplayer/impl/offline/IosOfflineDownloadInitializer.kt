// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/**
 * At launch, reconnects to transfers that carried on while the app wasn't running, then resumes
 * downloads a previous run left unfinished.
 */
@ContributesIntoSet(AppScope::class)
@Inject
class IosOfflineDownloadInitializer(
  private val offlineDownloadManager: OfflineDownloadManager,
) : AppInitializer {

  override val priority: Int = AppInitializer.LOWEST_PRIORITY

  override suspend fun onInitialize() {
    BackgroundDownloadSession.connect()
    offlineDownloadManager.resumeDownloads()
  }
}
