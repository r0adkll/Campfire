package app.campfire.core.lifecycle

import kotlinx.coroutines.flow.StateFlow

interface AppLifecycleObserver {
  val state: StateFlow<AppLifecycleState>
}

enum class AppLifecycleState {
  Foreground,
  Background,
}
