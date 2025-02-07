package app.campfire.shake

data class AccelerometerEvent(
  val x: Double,
  val y: Double,
  val z: Double,
  val timestamp: Long,
)
