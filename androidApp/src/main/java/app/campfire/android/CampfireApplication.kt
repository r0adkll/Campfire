package app.campfire.android

import android.app.Application
import app.campfire.android.di.AndroidAppComponent
import app.campfire.android.logging.AndroidBark
import app.campfire.core.di.ComponentHolder
import app.campfire.core.logging.Heartwood
import kimchi.merge.app.campfire.android.di.createAndroidAppComponent

class CampfireApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    Heartwood.grow(AndroidBark())

    ComponentHolder.components += AndroidAppComponent.createAndroidAppComponent(this)
  }
}
