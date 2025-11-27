package app.campfire.ui.theming.theme

import com.r0adkll.swatchbuckler.compose.Theme

data class ComputedTheme(
  val key: String,
  val cacheKey: String,
  val theme: Theme,
)
