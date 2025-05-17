package app.campfire.android.di

import android.app.Application
import android.os.Build
import app.campfire.android.BuildConfig
import app.campfire.common.di.SharedAppComponent
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.Flavor
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
@DependencyGraph(AppScope::class, isExtendable = true)
interface AndroidAppComponent : SharedAppComponent {

  @DependencyGraph.Factory
  interface Factory {
    fun create(@Provides application: Application): AndroidAppComponent
  }

  @Suppress("DEPRECATION")
  @SingleIn(AppScope::class)
  @Provides
  fun provideApplicationInfo(application: Application): ApplicationInfo {
    val packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)

    return ApplicationInfo(
      packageName = application.packageName,
      debugBuild = BuildConfig.DEBUG,
      flavor = Flavor.Standard,
      versionName = packageInfo.versionName ?: "unknown",
      versionCode = packageInfo.versionCode,
      osName = "Android",
      osVersion = Build.VERSION.SDK_INT.toString(),
      manufacturer = Build.MANUFACTURER,
      model = Build.MODEL,
      sdkVersion = Build.VERSION.SDK_INT,
    )
  }
}
