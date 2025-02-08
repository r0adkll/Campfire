package app.campfire.shake

enum class ShakeSensitivity(val value: Double) {
  VeryLow(ShakeSensitivityMagnitudes.veryLow),
  Low(ShakeSensitivityMagnitudes.low),
  Medium(ShakeSensitivityMagnitudes.medium),
  High(ShakeSensitivityMagnitudes.high),
  VeryHigh(ShakeSensitivityMagnitudes.veryHigh),
  ;

  val valueSquared: Double get() = value * value

  companion object {
    val Default = Medium
  }
}

expect object ShakeSensitivityMagnitudes {
  val veryLow: Double
  val low: Double
  val medium: Double
  val high: Double
  val veryHigh: Double
}
