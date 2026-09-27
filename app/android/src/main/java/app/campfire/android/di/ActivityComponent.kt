// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android.di

import android.app.Activity
import androidx.core.os.ConfigurationCompat
import app.campfire.account.api.UserSessionManager
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.common.root.CampfireContent
import app.campfire.core.ComponentActivityPlugin
import app.campfire.core.di.AppScope
import app.campfire.core.di.UiScope
import app.campfire.core.permission.LocalNetworkPermissionController
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import java.util.Locale

@GraphExtension(UiScope::class)
interface ActivityComponent {
  val campfireContent: CampfireContent
  val componentActivityPlugins: Set<ComponentActivityPlugin>
  val offlineDownloadManager: OfflineDownloadManager
  val userSessionManager: UserSessionManager
  val localNetworkPermission: LocalNetworkPermissionController

  @Provides
  fun provideActivityLocale(activity: Activity): Locale {
    return ConfigurationCompat.getLocales(activity.resources.configuration)
      .get(0) ?: Locale.getDefault()
  }

  @ContributesTo(AppScope::class)
  @GraphExtension.Factory
  interface Factory {
    fun create(@Provides activity: Activity): ActivityComponent
  }
}
