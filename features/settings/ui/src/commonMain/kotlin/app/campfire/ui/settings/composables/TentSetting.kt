package app.campfire.ui.settings.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.icon
import app.campfire.core.model.Tent

private val TentIconSize = 48.dp

@Composable
internal fun TentSetting(
  tent: Tent,
  onTentChange: (Tent) -> Unit,
  modifier: Modifier = Modifier,
) {
  var isExpanded by remember { mutableStateOf(false) }
  SettingListItem(
    headlineContent = { Text("Tent") },
    supportingContent = { Text("Pick your campsites tent & theme") },
    trailingContent = {
      Box {
        Image(
          tent.icon,
          contentDescription = null,
          modifier = Modifier
            .clickable {
              isExpanded = true
            }
            .size(TentIconSize),
        )

        DropdownMenu(
          expanded = isExpanded,
          onDismissRequest = { isExpanded = false },
          modifier = Modifier.padding(
            horizontal = 8.dp,
            vertical = 8.dp,
          ),
        ) {
          Tent.entries.forEach { tentOption ->
            Image(
              tentOption.icon,
              contentDescription = null,
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                  onTentChange(tentOption)
                  isExpanded = false
                },
            )
          }
        }
      }
    },
    modifier = modifier
      .clickable { isExpanded = true },
  )
}
