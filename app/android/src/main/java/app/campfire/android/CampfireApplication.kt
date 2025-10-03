package app.campfire.android

import android.app.Application
import app.campfire.android.di.AndroidAppComponent
import app.campfire.android.logging.AndroidBark
import app.campfire.core.di.ComponentHolder
import app.campfire.core.logging.Heartwood
import app.campfire.core.logging.bark
import kimchi.merge.app.campfire.android.di.createAndroidAppComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class CampfireApplication : Application() {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  override fun onCreate() {
    super.onCreate()
    if (BuildConfig.DEBUG) {
      Heartwood.grow(AndroidBark())
    }

    // Create application component
    val component = AndroidAppComponent.createAndroidAppComponent(this).also {
      ComponentHolder.components += it
    }

    // Create initial UserComponent
    val sessionManager = component.sessionManager
    val userComponent = component.userComponentManager
      .getOrCreateUserComponent(sessionManager.current).also {
        ComponentHolder.components += it
      }

    // Call startup initializers
    component.startupInitializer.initialize()

    // Observe any user session changes, and re-create the scope
    scope.launch {
      sessionManager.observe()
        .drop(1) // Make sure we don't double initialize our initial userComponent
        .collect { userSession ->
          bark { "User session changed: $userSession" }
          val newUserComponent = component.userComponentManager.getOrCreateUserComponent(userSession)
          ComponentHolder.updateComponent(this, newUserComponent)
        }
    }
  }
}
