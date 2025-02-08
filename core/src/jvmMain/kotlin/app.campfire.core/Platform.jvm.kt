package app.campfire.core

actual val currentPlatform: Platform = Platform.DESKTOP

// TODO: Figure out a way to do debug/release flavors for desktop
actual val isDebug: Boolean
  get() = true
