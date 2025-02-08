package app.campfire.shake

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.time.FatherTime
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import me.tatarka.inject.annotations.Provides
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue

actual class ShakeDetector(
  private val fatherTime: FatherTime,
) {

  private var listener: Listener? = null

  private val motionManager = CMMotionManager()
  private val samplingShakeDetector = SamplingShakeDetector {
    listener?.onShake()
  }

  actual val isAvailable: Boolean
    get() = motionManager.isAccelerometerAvailable()

  @OptIn(ExperimentalForeignApi::class)
  actual fun start(sensitivity: ShakeSensitivity, listener: Listener) {
    this.listener = listener
    samplingShakeDetector.clear()
    samplingShakeDetector.sensitivity = sensitivity

    if (!motionManager.isAccelerometerAvailable()) {
      bark(LogPriority.WARN) { "Accelerometer not available" }
      return
    }

    motionManager.accelerometerUpdateInterval = 1.0 / 50.0 // 50hz
    motionManager.startAccelerometerUpdatesToQueue(NSOperationQueue.mainQueue, { data, error ->
      val event = data?.acceleration?.useContents {
        AccelerometerEvent(
          x = x,
          y = y,
          z = z,
          timestamp = fatherTime.nowInEpochMillis(),
        )
      }

      if (event == null) return@startAccelerometerUpdatesToQueue

      samplingShakeDetector.addAccelerometerEvent(event)
    })
  }

  actual fun stop() {
    motionManager.stopAccelerometerUpdates()
    samplingShakeDetector.clear()
  }

  actual fun interface Listener {
    actual fun onShake()
  }
}

actual interface ShakeDetectorPlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideIosShakeDetector(
    fatherTime: FatherTime,
  ): ShakeDetector = ShakeDetector(fatherTime)
}
