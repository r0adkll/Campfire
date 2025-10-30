package app.campfire.ui.settings.composables

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
internal fun SwitchSetting(
  value: Boolean,
  onValueChange: (Boolean) -> Unit,
  headlineContent: @Composable () -> Unit,
  supportingContent: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  leadingContent: @Composable (() -> Unit)? = null,
) {
  SettingListItem(
    headlineContent = headlineContent,
    supportingContent = supportingContent,
    leadingContent = leadingContent,
    trailingContent = {
      Switch(
        checked = value,
        onCheckedChange = onValueChange,
      )
    },
    colors = ListItemDefaults.colors(
      containerColor = Color.Transparent,
    ),
    modifier = modifier
      .clickable { onValueChange(!value) },
  )
}
