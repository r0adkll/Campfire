// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import android.app.Application
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.settings.api.MobileDataSettings
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

/**
 * The network downloads may use: any, or only unmetered (Wi-Fi) when [wifiOnly]. With the latter,
 * queued and running downloads pause on mobile data and resume on Wi-Fi.
 */
@OptIn(UnstableApi::class)
internal fun downloadRequirements(wifiOnly: Boolean): Requirements = Requirements(
  if (wifiOnly) Requirements.NETWORK_UNMETERED else Requirements.NETWORK,
)

/**
 * Applies changes to "Download on Wi-Fi only" to the running download manager. The initial value is
 * set when the manager is created; changes only come from Settings, i.e. with the app in the
 * foreground, where starting the download service to deliver them is allowed.
 */
@ContributesMultibinding(AppScope::class)
@Inject
class DownloadRequirementsObserver(
  private val application: Application,
  private val mobileDataSettings: MobileDataSettings,
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
  }
}
