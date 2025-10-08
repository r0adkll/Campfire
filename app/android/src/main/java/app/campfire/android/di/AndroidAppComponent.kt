package app.campfire.android.di

import android.app.Application
import android.os.Build
import app.campfire.android.BuildConfig
import app.campfire.common.di.SharedAppComponent
import app.campfire.core.ComponentActivityPlugin
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.Flavor
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import com.r0adkll.kimchi.annotations.MergeComponent
import me.tatarka.inject.annotations.Provides

@SingleIn(AppScope::class)
@MergeComponent(AppScope::class)
abstract class AndroidAppComponent(
  @get:Provides val application: Application,
) : SharedAppComponent {

  abstract val componentActivityPlugins: Set<ComponentActivityPlugin>

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

  companion object
}
