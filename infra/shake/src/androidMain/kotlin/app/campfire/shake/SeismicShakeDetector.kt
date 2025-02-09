package app.campfire.shake

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SeismicShakeDetector(
  private val listener: Listener,
) : SensorEventListener {

  /** Listens for shakes.  */
  interface Listener {
    /** Called on the main thread when the device is shaken.  */
    fun hearShake()
  }

  private val samplingShakeDetector = SamplingShakeDetector {
    listener.hearShake()
  }

  private var sensorManager: SensorManager? = null
  private var accelerometer: Sensor? = null

  val isRunning: Boolean get() = accelerometer != null

  /**
   * Starts listening for shakes on devices with appropriate hardware.
   * Allowing to set the sensor delay, available values are:
   * SENSOR_DELAY_FASTEST, SENSOR_DELAY_GAME, SENSOR_DELAY_UI, SENSOR_DELAY_NORMAL.
   * @see [SensorManager](https://developer.android.com/reference/android/hardware/SensorManager)
   *
   * @return true if the device supports shake detection.
   */
  @JvmOverloads
  fun start(
    sensorManager: SensorManager,
    sensorDelay: Int = SensorManager.SENSOR_DELAY_NORMAL,
  ): Boolean {
    // Already started?
    if (accelerometer != null) {
      return true
    }

    accelerometer = sensorManager.getDefaultSensor(
      Sensor.TYPE_ACCELEROMETER,
    )

    // If this phone has an accelerometer, listen to it.
    if (accelerometer != null) {
      this.sensorManager = sensorManager
      sensorManager.registerListener(this, accelerometer, sensorDelay)
    }
    return accelerometer != null
  }

  /**
   * Stops listening.  Safe to call when already stopped.  Ignored on devices
   * without appropriate hardware.
   */
  fun stop() {
    if (accelerometer != null) {
      samplingShakeDetector.clear()
      sensorManager?.unregisterListener(this, accelerometer)
      sensorManager = null
      accelerometer = null
    }
  }

  override fun onSensorChanged(event: SensorEvent) {
    val accelerometerEvent = AccelerometerEvent(
      event.values[0].toDouble(),
      event.values[1].toDouble(),
      event.values[2].toDouble(),
      event.timestamp,
    )

    samplingShakeDetector.addAccelerometerEvent(accelerometerEvent)
  }

  /** Sets the acceleration threshold sensitivity.  */
  fun setSensitivity(sensitivity: ShakeSensitivity) {
    samplingShakeDetector.sensitivity = sensitivity
  }

  override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
  }
}
