package app.campfire.ui.settings.composables

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
internal fun ActionSetting(
  headlineContent: @Composable () -> Unit,
  supportingContent: (@Composable () -> Unit)? = null,
  leadingContent: (@Composable () -> Unit)? = null,
  trailingContent: (@Composable () -> Unit)? = null,
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  SettingListItem(
    leadingContent = leadingContent,
    headlineContent = headlineContent,
    supportingContent = supportingContent,
    trailingContent = trailingContent,
    colors = ListItemDefaults.colors(
      containerColor = Color.Transparent,
    ),
    modifier = modifier
      .clickable(
        enabled = onClick != null,
        onClick = {
          onClick?.invoke()
        },
      ),
  )
}
