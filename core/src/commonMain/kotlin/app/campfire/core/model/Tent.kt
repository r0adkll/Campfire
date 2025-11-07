package app.campfire.core.model

/**
 * The type/color of tent that represents a given server
 */
enum class Tent {
  Red,
  Blue,
  Green,
  Yellow,
  Orange,
  Purple,
  ;

  companion object {
    val Default = Red
  }
}
