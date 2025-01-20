package app.campfire.common.compose.layout

import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import app.campfire.common.compose.theme.ElevationTokens

enum class ContentLayout {
  Root,
  Supporting,
}

val LocalContentLayout = staticCompositionLocalOf<ContentLayout> {
  error("No content layout provided in this composition")
}

val ContentLayout.cardElevation: CardElevation
  @Composable get() {
    return when (this) {
      ContentLayout.Root -> CardDefaults.elevatedCardElevation()
      ContentLayout.Supporting -> CardDefaults.elevatedCardElevation(
        defaultElevation = ElevationTokens.Level2,
        pressedElevation = ElevationTokens.Level2,
        focusedElevation = ElevationTokens.Level2,
        hoveredElevation = ElevationTokens.Level3,
        draggedElevation = ElevationTokens.Level4,
        disabledElevation = ElevationTokens.Level2,
      )
    }
  }
