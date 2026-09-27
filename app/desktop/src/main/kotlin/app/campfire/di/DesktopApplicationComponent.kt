// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.di

import androidx.compose.ui.unit.Density
import app.campfire.BuildConfig
import app.campfire.common.di.SharedAppComponent
import app.campfire.config.FileSystemPreferences
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.Flavor
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import java.util.prefs.Preferences

@DependencyGraph(AppScope::class)
interface DesktopApplicationComponent : SharedAppComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideApplicationId(): ApplicationInfo = ApplicationInfo(
    packageName = "app.campfire",
    debugBuild = BuildConfig.DEBUG,
    flavor = Flavor.Standard,
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE,
    osName = System.getProperty("os.name"),
    osVersion = System.getProperty("os.version"),
  )

  @SingleIn(AppScope::class)
  @Provides
  fun providePreferences(): Preferences {
    return FileSystemPreferences.getUserRoot(
      fileName = "config.properties",
      applicationDir = ".config/Campfire",
    )
  }

  @Provides
  fun provideDensity(): Density = Density(density = 1f) // FIXME
}
