package app.campfire.shake

import app.campfire.core.di.AppScope
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

actual class ShakeDetector {

  actual val isAvailable: Boolean = false
  actual val isRunning: Boolean = false

  actual fun start(sensitivity: ShakeSensitivity, listener: Listener) {
    // No-op
  }

  actual fun stop() {
    // No-op
  }

  actual fun interface Listener {
    actual fun onShake()
  }
}

actual interface ShakeDetectorPlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideAndroidShakeDetector(): ShakeDetector = ShakeDetector()
}
