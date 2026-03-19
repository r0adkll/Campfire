package app.campfire.core

actual val currentPlatform: Platform = Platform.ANDROID

actual val isDebug: Boolean
  get() = BuildConfig.DEBUG
