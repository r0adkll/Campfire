package app.campfire.shake

import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesTo

expect class ShakeDetector {

  val isAvailable: Boolean
  val isRunning: Boolean

  fun start(
    sensitivity: ShakeSensitivity,
    listener: Listener,
  )

  fun stop()

  fun interface Listener {
    fun onShake()
  }
}

expect interface ShakeDetectorPlatformComponent

@ContributesTo(AppScope::class)
interface ShakeDetectorComponent : ShakeDetectorPlatformComponent
