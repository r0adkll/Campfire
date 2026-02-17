package app.campfire.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable

@Composable
internal fun HomeNavigationItemIcon(item: HomeNavigationItem, selected: Boolean) {
  val selectedImageVector = item.selectedImageVector
  if (selectedImageVector != null) {
    Crossfade(targetState = selected) { s ->
      Icon(
        imageVector = if (s) selectedImageVector else item.iconImageVector,
        contentDescription = item.contentDescription,
      )
    }
  } else {
    Icon(
      imageVector = item.iconImageVector,
      contentDescription = item.contentDescription,
    )
  }
}
