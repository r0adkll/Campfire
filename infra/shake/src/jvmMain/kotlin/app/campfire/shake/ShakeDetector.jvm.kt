package app.campfire.shake

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import me.tatarka.inject.annotations.Provides

actual class ShakeDetector {

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
