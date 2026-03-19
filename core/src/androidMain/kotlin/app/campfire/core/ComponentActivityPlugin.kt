package app.campfire.core

import androidx.activity.ComponentActivity
import androidx.annotation.MainThread

interface ComponentActivityPlugin {
  @MainThread
  fun register(activity: ComponentActivity)

  @MainThread
  fun unregister()
}
