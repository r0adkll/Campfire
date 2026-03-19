package app.campfire.core

import android.content.pm.ApplicationInfo

actual val currentPlatform: Platform = Platform.ANDROID

actual val isDebug: Boolean by lazy {
  try {
    val clazz = Class.forName("android.app.ActivityThread")
    val app = clazz.getMethod("currentApplication").invoke(null) as? android.app.Application
    (app?.applicationInfo?.flags?.and(ApplicationInfo.FLAG_DEBUGGABLE) ?: 0) != 0
  } catch (_: Exception) {
    false
  }
}
