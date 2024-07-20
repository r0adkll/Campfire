package app.campfire.android

import android.app.Application
import app.campfire.android.di.AndroidAppComponent
import app.campfire.android.logging.AndroidBark
import app.campfire.core.extensions.unsafeLazy
import app.campfire.core.logging.Heartwood
import kotlininject.merge.app.campfire.android.di.MergedAndroidAppComponent
import kotlininject.merge.app.campfire.android.di.createMergedAndroidAppComponent

class CampfireApplication : Application() {

  val component: MergedAndroidAppComponent by unsafeLazy {
    AndroidAppComponent.createMergedAndroidAppComponent(this)
  }

  override fun onCreate() {
    super.onCreate()
    Heartwood.grow(AndroidBark())
  }
}
