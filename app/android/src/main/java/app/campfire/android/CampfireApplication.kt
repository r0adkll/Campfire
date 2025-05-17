package app.campfire.android

import android.app.Application
import app.campfire.android.di.AndroidAppComponent
import app.campfire.android.logging.AndroidBark
import app.campfire.core.di.ComponentHolder
import app.campfire.core.logging.Heartwood
import dev.zacsweers.metro.createGraphFactory

class CampfireApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    Heartwood.grow(AndroidBark())

    val component = createGraphFactory<AndroidAppComponent.Factory>()
      .create(this)
      .also {
        ComponentHolder.components += it
      }

    component.startupInitializer.initialize()
  }
}
