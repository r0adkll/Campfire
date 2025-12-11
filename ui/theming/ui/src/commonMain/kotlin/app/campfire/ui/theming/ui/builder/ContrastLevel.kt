package app.campfire.ui.theming.ui.builder

enum class ContrastLevel(val contrast: Float) {
  Normal(0f),
  Medium(0.5f),
  High(1f),
  ;

  companion object {
    fun from(value: Float): ContrastLevel {
      return entries.firstOrNull {
        it.contrast == value
      } ?: Normal
    }
  }
}
