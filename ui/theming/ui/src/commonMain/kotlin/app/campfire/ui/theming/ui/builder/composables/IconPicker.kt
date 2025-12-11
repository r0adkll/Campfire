package app.campfire.ui.theming.ui.builder.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.thenIf
import app.campfire.common.compose.icons.icon
import app.campfire.ui.theming.api.AppTheme

internal val IconSize = 48.dp

@Composable
internal fun IconPicker(
  icon: AppTheme.Icon,
  onIconClick: (AppTheme.Icon) -> Unit,
  modifier: Modifier = Modifier,
) {
  var isExpanded by remember { mutableStateOf(false) }
  Box(
    modifier = modifier,
  ) {
    Image(
      icon.icon(),
      contentDescription = "AppTheme Icon",
      modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .clickable {
          isExpanded = true
        }
        .size(IconSize),
    )

    DropdownMenu(
      expanded = isExpanded,
      onDismissRequest = { isExpanded = false },
      offset = DpOffset((-8).dp, -(IconSize + 8.dp)),
      containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
      modifier = Modifier.padding(
        horizontal = 8.dp,
      ),
    ) {
      AppTheme.Icon.entries.forEachIndexed { index, ico ->
        Image(
          ico.icon(),
          contentDescription = null,
          modifier = Modifier
            .thenIf(index == 0) { padding(bottom = 4.dp) }
            .thenIf(index == AppTheme.Icon.entries.lastIndex) { padding(top = 4.dp) }
            .thenIf(index > 0 && index < AppTheme.Icon.entries.lastIndex) {
              padding(vertical = 4.dp)
            }
            .clip(RoundedCornerShape(8.dp))
            .size(IconSize)
            .clickable {
              onIconClick(ico)
              isExpanded = false
            },
        )
      }
    }
  }
}
