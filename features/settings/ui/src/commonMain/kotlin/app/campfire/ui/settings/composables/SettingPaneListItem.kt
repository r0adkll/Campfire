package app.campfire.ui.settings.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SettingPaneListItem(
  icon: @Composable () -> Unit,
  title: @Composable () -> Unit,
  subtitle: (@Composable () -> Unit)? = null,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  selected: Boolean = false,
  shape: Shape = SettingsPaneDefaults.middleShape(),
) {
  ListItem(
    modifier = modifier
      .clip(if (selected) SettingsPaneDefaults.selectedShape() else shape)
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
        CompositionLocalProvider(
          LocalContentColor provides MaterialTheme.colorScheme.primary,
        ) {
          icon()
        }
      }
    },
    colors = ListItemDefaults.colors(
      containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
      } else {
        MaterialTheme.colorScheme.surfaceContainer
      },
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

object SettingsPaneDefaults {

  private val largeRadius = 20.dp
  private val smallRadius = 4.dp

  val ContentSpacing: Dp = 2.dp

  @Composable
  fun topShape(): Shape = RoundedCornerShape(
    topStart = largeRadius,
    topEnd = largeRadius,
    bottomStart = smallRadius,
    bottomEnd = smallRadius,
  )

  @Composable
  fun middleShape(): Shape = RoundedCornerShape(smallRadius)

  @Composable
  fun selectedShape(): Shape = MaterialTheme.shapes.large

  @Composable
  fun bottomShape(): Shape = RoundedCornerShape(
    topStart = smallRadius,
    topEnd = smallRadius,
    bottomStart = largeRadius,
    bottomEnd = largeRadius,
  )
}
