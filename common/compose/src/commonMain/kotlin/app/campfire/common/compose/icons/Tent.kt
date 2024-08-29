package app.campfire.common.compose.icons

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import app.campfire.core.model.Tent

/**
 * Get the [ImageVector] for a given tent
 */
val Tent.icon: ImageVector get() = when (this) {
  Tent.Red -> Icons.TentRed
  Tent.Blue -> Icons.TentBlue
  Tent.Green -> Icons.TentGreen
  Tent.Yellow -> Icons.TentYellow
  Tent.Orange -> Icons.TentOrange
  Tent.Purple -> Icons.TentPurple
}
