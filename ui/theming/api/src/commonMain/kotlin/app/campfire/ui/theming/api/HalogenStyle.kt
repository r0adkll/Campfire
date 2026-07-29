package app.campfire.ui.theming.api

/**
 * One of Halogen's eight built-in aesthetic presets. Each preset bundles chroma caps and
 * prompt guidance that steer the on-device LLM toward a particular visual style. See
 * `halogen.HalogenConfig.presets` for the source mapping.
 */
enum class HalogenStyle(val displayName: String) {
  Default("Default"),
  Vibrant("Vibrant"),
  Muted("Muted"),
  Monochrome("Monochrome"),
  Punchy("Punchy"),
  Pastel("Pastel"),
  Editorial("Editorial"),
  Expressive("Expressive"),
}
