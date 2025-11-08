package app.campfire.android

import android.app.Application
import app.campfire.android.di.AndroidAppComponent
import app.campfire.android.logging.AndroidBark
import app.campfire.core.di.ComponentHolder
import app.campfire.core.logging.Heartwood
import app.campfire.tracing.trace
import kimchi.merge.app.campfire.android.di.createAndroidAppComponent

class CampfireApplication : Application() {

  override fun onCreate() = trace("CampfireApplication.onCreate") {
    super.onCreate()
    if (BuildConfig.DEBUG) {
      Heartwood.grow(AndroidBark())
    }

    // Create application component
    val component = trace("createAndroidAppComponent") {
      AndroidAppComponent.createAndroidAppComponent(this)
        .also {
          ComponentHolder.components += it
        }
    }

    // Call startup initializers
    component.startupInitializer.initialize()
  }
}
