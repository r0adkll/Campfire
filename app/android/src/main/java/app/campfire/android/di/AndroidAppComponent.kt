package app.campfire.android.di

import android.app.Application
import android.os.Build
import app.campfire.android.BuildConfig
import app.campfire.audioplayer.impl.MediaControllerConnector
import app.campfire.common.di.SharedAppComponent
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.Flavor
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForAppScope
import app.campfire.settings.api.DevSettings
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@SingleIn(AppScope::class)
@DependencyGraph(AppScope::class, isExtendable = true)
interface AndroidAppComponent : SharedAppComponent {

  @DependencyGraph.Factory
  interface Factory {
    fun create(@Provides application: Application): AndroidAppComponent
  }

  // FIXME: Needed for https://github.com/ZacSweers/metro/issues/377
  val mediaControllerConnector: MediaControllerConnector

  // FIXME: Needed for https://github.com/ZacSweers/metro/issues/377
  val devSettings: DevSettings

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
