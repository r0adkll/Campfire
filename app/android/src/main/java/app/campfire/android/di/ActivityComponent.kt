package app.campfire.android.di

import android.app.Activity
import androidx.core.os.ConfigurationCompat
import app.campfire.audioplayer.impl.MediaControllerConnector
import app.campfire.audioplayer.impl.cast.MediaRouterCastController
import app.campfire.common.root.CampfireContent
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UiScope
import com.r0adkll.kimchi.annotations.ContributesSubcomponent
import java.util.Locale
import me.tatarka.inject.annotations.Provides

@SingleIn(UiScope::class)
@ContributesSubcomponent(
  scope = UiScope::class,
  parentScope = AppScope::class,
)
interface ActivityComponent {
  val campfireContent: CampfireContent
  val mediaControllerConnector: MediaControllerConnector
  val mediaRouterCastController: MediaRouterCastController

  @Provides
  fun provideActivityLocale(activity: Activity): Locale {
    return ConfigurationCompat.getLocales(activity.resources.configuration)
      .get(0) ?: Locale.getDefault()
  }

  @ContributesSubcomponent.Factory
  interface Factory {
    fun create(activity: Activity): ActivityComponent
  }
}
