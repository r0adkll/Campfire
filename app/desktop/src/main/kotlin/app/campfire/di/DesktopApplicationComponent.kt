// Copyright 2023, Google LLC, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.di

import androidx.compose.ui.unit.Density
import app.campfire.common.di.SharedAppComponent
import app.campfire.config.FileSystemPreferences
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.Flavor
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForAppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import java.util.prefs.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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

  // FIXME: https://github.com/ZacSweers/metro/pull/407
  //  Fixed in Kotlin 2.2.0 + Future Metro version
  //  Should probably re-think about how we approach DI scoped primitives like CoroutineScope and the like
  // HACK to get building
  @SingleIn(AppScope::class)
  @Provides
  @ForAppScope
  fun provideApplicationCoroutineScope(
    dispatcherProvider: DispatcherProvider,
  ): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
