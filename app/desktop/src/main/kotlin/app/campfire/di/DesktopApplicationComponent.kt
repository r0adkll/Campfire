// Copyright 2023, Google LLC, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.di

import androidx.compose.ui.unit.Density
import app.campfire.common.di.SharedAppComponent
import app.campfire.config.FileSystemPreferences
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.Flavor
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import java.util.prefs.Preferences

@SingleIn(AppScope::class)
@DependencyGraph(AppScope::class, isExtendable = true)
interface DesktopApplicationComponent : SharedAppComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideApplicationId(): ApplicationInfo = ApplicationInfo(
    packageName = "app.campfire",
    debugBuild = true,
    flavor = Flavor.Standard,
    versionName = "1.0.0",
    versionCode = 1,
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
