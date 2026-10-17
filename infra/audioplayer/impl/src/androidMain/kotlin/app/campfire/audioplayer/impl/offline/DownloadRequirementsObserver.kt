// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import android.app.Application
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.network.reachability.WorkOffline
import app.campfire.settings.api.MobileDataSettings
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The network downloads may use: any, or only unmetered (Wi-Fi) when [wifiOnly]. With the latter,
 * queued and running downloads pause on mobile data and resume on Wi-Fi.
 */
@OptIn(UnstableApi::class)
internal fun downloadRequirements(wifiOnly: Boolean): Requirements = Requirements(
  if (wifiOnly) Requirements.NETWORK_UNMETERED else Requirements.NETWORK,
)

/**
 * Applies changes to "Download on Wi-Fi only" and working offline to the running download manager:
 * working offline pauses every download until it ends. The initial values are set when the
 * manager is created. "Download on Wi-Fi only" only changes from Settings, i.e. with the app in
 * the foreground, where starting the download service to deliver it is allowed.
 */
@OptIn(UnstableApi::class)
@ContributesIntoSet(AppScope::class)
@Inject
class DownloadRequirementsObserver(
  private val application: Application,
  private val mobileDataSettings: MobileDataSettings,
  private val workOffline: WorkOffline,
  private val downloadManager: Lazy<DownloadManager>,
  @ForScope(AppScope::class) private val scope: CoroutineScope,
) : AppInitializer {

  @OptIn(UnstableApi::class)
  override suspend fun onInitialize() {
    scope.launch {
      mobileDataSettings.observeDownloadOnWifiOnly()
        // The first value is the one the manager was created with
        .drop(1)
        .collect { wifiOnly ->
          runCatching {
            DownloadService.sendSetRequirements(
              application,
              CampfireDownloadService::class.java,
              downloadRequirements(wifiOnly),
              false,
            )
          }.onFailure { bark(LogPriority.ERROR, throwable = it) { "Unable to update download requirements" } }
        }
    }
    scope.launch {
      workOffline.state
        .map { it.isOffline }
        .distinctUntilChanged()
        // The first value is the one the manager was created with
        .drop(1)
        .collect { offline ->
          // Directly on the manager rather than through the service: this also changes with the
          // network, possibly in the background where the service can't be started. Resuming
          // still goes through the service when it can, so it runs in the foreground.
          withContext(Dispatchers.Main) {
            if (offline) {
              downloadManager.value.pauseDownloads()
            } else {
              runCatching {
                DownloadService.sendResumeDownloads(application, CampfireDownloadService::class.java, false)
              }.onFailure {
                bark(LogPriority.WARN, throwable = it) { "Download service unavailable; resuming in place" }
                downloadManager.value.resumeDownloads()
              }
            }
          }
        }
    }
  }
}
