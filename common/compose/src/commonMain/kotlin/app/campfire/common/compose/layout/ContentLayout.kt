package app.campfire.common.compose.layout

import androidx.compose.runtime.staticCompositionLocalOf

enum class ContentLayout {
  Root,
  Supporting,
}

val LocalContentLayout = staticCompositionLocalOf<ContentLayout> {
  error("No content layout provided in this composition")
}
