// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android.di

import android.app.Application
import android.os.Build
import app.campfire.android.BuildConfig
import app.campfire.common.di.SharedAppComponent
import app.campfire.core.ComponentActivityPlugin
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.Flavor
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@DependencyGraph(AppScope::class)
interface AndroidAppComponent : SharedAppComponent {

  val componentActivityPlugins: Set<ComponentActivityPlugin>

  @Suppress("DEPRECATION")
  @SingleIn(AppScope::class)
  @Provides
  fun provideApplicationInfo(application: Application): ApplicationInfo {
    val packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)

    return ApplicationInfo(
      packageName = application.packageName,
      debugBuild = BuildConfig.DEBUG,
      flavor = when (BuildConfig.FLAVOR) {
        "standard" -> Flavor.Standard
        "foss" -> Flavor.Foss
        "beta" -> Flavor.Beta
        else -> Flavor.Alpha
      },
      versionName = packageInfo.versionName ?: "unknown",
      versionCode = packageInfo.versionCode,
      osName = "Android",
      osVersion = Build.VERSION.SDK_INT.toString(),
      manufacturer = Build.MANUFACTURER,
      model = Build.MODEL,
      sdkVersion = Build.VERSION.SDK_INT,
    )
  }

  @DependencyGraph.Factory
  fun interface Factory {
    fun create(@Provides application: Application): AndroidAppComponent
  }
}
