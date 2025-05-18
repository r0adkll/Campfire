package app.campfire.android.di

import android.app.Activity
import androidx.core.os.ConfigurationCompat
import app.campfire.common.root.CampfireContentProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.UiScope
import dev.zacsweers.metro.ContributesGraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import java.util.Locale

@SingleIn(UiScope::class)
@ContributesGraphExtension(UiScope::class)
interface ActivityComponent {
  val campfireContentProvider: CampfireContentProvider

  // FIXME: https://github.com/ZacSweers/metro/issues/377
  //  can't have this provision here AND in AndroidAppComponent
//  val mediaControllerConnector: MediaControllerConnector

  @Provides
  fun provideActivityLocale(activity: Activity): Locale {
    return ConfigurationCompat.getLocales(activity.resources.configuration)
      .get(0) ?: Locale.getDefault()
  }

  @ContributesGraphExtension.Factory(AppScope::class)
  interface Factory {
    fun create(@Provides activity: Activity): ActivityComponent
  }
}
