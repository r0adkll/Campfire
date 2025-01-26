package app.campfire.common.compose.icons

import androidx.compose.ui.graphics.vector.ImageVector
import app.campfire.core.model.Tent

/**
 * Get the [ImageVector] for a given tent
 */
val Tent.icon: ImageVector get() = when (this) {
  Tent.Red -> CampfireIcons.Tents.Red
  Tent.Blue -> CampfireIcons.Tents.Blue
  Tent.Green -> CampfireIcons.Tents.Green
  Tent.Yellow -> CampfireIcons.Tents.Yellow
  Tent.Orange -> CampfireIcons.Tents.Orange
  Tent.Purple -> CampfireIcons.Tents.Purple
}
