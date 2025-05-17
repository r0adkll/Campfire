package app.campfire.shake

import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesTo

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
