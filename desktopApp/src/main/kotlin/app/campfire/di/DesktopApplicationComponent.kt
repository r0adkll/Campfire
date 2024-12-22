// Copyright 2023, Google LLC, Christopher Banes and the Tivi project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.di

import androidx.compose.ui.unit.Density
import app.campfire.config.FileSystemPreferences
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.Flavor
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.shared.di.SharedAppComponent
import com.r0adkll.kimchi.annotations.MergeComponent
import java.util.prefs.Preferences
import me.tatarka.inject.annotations.Provides

@SingleIn(AppScope::class)
@MergeComponent(AppScope::class)
abstract class DesktopApplicationComponent : SharedAppComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideApplicationId(): ApplicationInfo = ApplicationInfo(
    packageName = "app.campfire",
    debugBuild = true,
    flavor = Flavor.Standard,
    versionName = "1.0.0",
    versionCode = 1,
    osName = "JVM",
    osVersion = "1",
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

  companion object
}
