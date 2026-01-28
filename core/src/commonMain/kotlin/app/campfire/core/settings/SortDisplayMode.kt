package app.campfire.core.settings

interface SortDisplayMode {
  enum class Mode {
    Alphabetical,
    Numerical,
    Normal,
  }

  val mode: Mode
}
