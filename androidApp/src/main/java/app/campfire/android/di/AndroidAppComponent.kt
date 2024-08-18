package app.campfire.android.di

import android.app.Application
import app.campfire.android.BuildConfig
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.Flavor
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.shared.di.SharedAppComponent
import com.r0adkll.kimchi.annotations.MergeComponent
import me.tatarka.inject.annotations.Provides

@SingleIn(AppScope::class)
@MergeComponent(AppScope::class)
abstract class AndroidAppComponent(
  @get:Provides val application: Application,
) : SharedAppComponent {

  @Suppress("DEPRECATION")
  @SingleIn(AppScope::class)
  @Provides
  fun provideApplicationInfo(application: Application): ApplicationInfo {
    val packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)

    return ApplicationInfo(
      packageName = application.packageName,
      debugBuild = BuildConfig.DEBUG,
      flavor = Flavor.Standard,
      versionName = packageInfo.versionName,
      versionCode = packageInfo.versionCode,
    )
  }

  companion object
}
