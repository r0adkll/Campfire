package app.campfire.ui.settings.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

@Composable
fun SettingPaneListItem(
  icon: @Composable () -> Unit,
  title: @Composable () -> Unit,
  subtitle: (@Composable () -> Unit)? = null,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  selected: Boolean = false,
) {
  ListItem(
    modifier = modifier
      .clip(CircleShape)
      .clickable(onClick = onClick),
    headlineContent = {
      ProvideTextStyle(MaterialTheme.typography.titleLarge) {
        title()
      }
    },
    supportingContent = subtitle,
    leadingContent = icon,
    colors = ListItemDefaults.colors(
      containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
      leadingIconColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
      } else {
        MaterialTheme.colorScheme.onSurface
      },
      headlineColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
      } else {
        MaterialTheme.colorScheme.onSurface
      },
      supportingColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
      } else {
        MaterialTheme.colorScheme.onSurfaceVariant
      },
    ),
  )
}
