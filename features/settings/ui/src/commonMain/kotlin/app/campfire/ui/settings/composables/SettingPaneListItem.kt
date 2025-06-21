package app.campfire.ui.settings.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
      ProvideTextStyle(MaterialTheme.typography.titleMedium) {
        title()
      }
    },
    supportingContent = subtitle,
    leadingContent = {
      Box(
        modifier = Modifier
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer)
          .size(40.dp),
        contentAlignment = Alignment.Center,
      ) {
        icon()
      }
    },
    colors = ListItemDefaults.colors(
      containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
      leadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
