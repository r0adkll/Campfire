package app.campfire.shake

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import me.tatarka.inject.annotations.Provides

actual class ShakeDetector(
  context: Context,
) : SeismicShakeDetector.Listener {
  private val sensorManager = context.getSystemService(SensorManager::class.java)
  private val seismicShakeDetector = SeismicShakeDetector(this)

  private var listener: Listener? = null

  actual val isAvailable: Boolean
    get() = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

  actual val isRunning: Boolean
    get() = seismicShakeDetector.isRunning

  actual fun start(sensitivity: ShakeSensitivity, listener: Listener) {
    this.listener = listener
    seismicShakeDetector.setSensitivity(sensitivity)
    seismicShakeDetector.start(sensorManager)
  }

  actual fun stop() {
    listener = null
    seismicShakeDetector.stop()
  }

  actual fun interface Listener {

    actual fun onShake()
  }

  override fun hearShake() {
    listener?.onShake()
  }
}

actual interface ShakeDetectorPlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideAndroidShakeDetector(application: Application): ShakeDetector = ShakeDetector(application)
}
