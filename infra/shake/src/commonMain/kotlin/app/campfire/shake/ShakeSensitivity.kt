package app.campfire.shake

enum class ShakeSensitivity(val value: Int) {
  VeryLow(10),
  Low(11),
  Medium(13),
  High(15),
  VeryHigh(16),
  ;

  val valueSquared: Int get() = value * value

  companion object {
    val Default = Medium
  }
}
