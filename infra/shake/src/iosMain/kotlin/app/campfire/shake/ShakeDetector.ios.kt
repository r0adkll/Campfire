package app.campfire.shake

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
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

  @OptIn(ExperimentalForeignApi::class)
  actual fun start(sensitivity: ShakeSensitivity, listener: Listener) {
    this.listener = listener
    samplingShakeDetector.clear()
    samplingShakeDetector.sensitivity = sensitivity

    motionManager.accelerometerUpdateInterval = 0.2
    motionManager.startAccelerometerUpdatesToQueue(NSOperationQueue.mainQueue, { data, error ->
      data?.acceleration?.useContents {
        val event = AccelerometerEvent(
          x = x,
          y = y,
          z = z,
          timestamp = fatherTime.nowInEpochMillis(),
        )

        samplingShakeDetector.addAccelerometerEvent(event)
      }
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
