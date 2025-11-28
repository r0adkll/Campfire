package app.campfire.ui.settings.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
import app.campfire.common.compose.icons.rememberTentVectorPainter
import app.campfire.core.model.Tent
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.setting_dynamic_colors_title
import org.jetbrains.compose.resources.stringResource

private val TentIconSize = 48.dp

@Composable
internal fun TentSetting(
  tent: Tent,
  onTentChange: (Tent) -> Unit,
  modifier: Modifier = Modifier,
  colors: ListItemColors = ListItemDefaults.colors(),
  enabled: Boolean = true,
) {
  var isExpanded by remember { mutableStateOf(false) }
  SettingListItem(
    colors = colors,
    headlineContent = {
      Text(
        text = "Tent",
        color = if (enabled) colors.headlineColor else colors.disabledHeadlineColor,
      )
    },
    supportingContent = {
      Text(
        text = "Pick your campsites tent & theme",
        color = if (enabled) colors.supportingTextColor else colors.disabledHeadlineColor,
      )
    },
    overlineContent = if (!enabled) {
      {
        Text(
          text = "Disable '${stringResource(Res.string.setting_dynamic_colors_title)}'",
          color = MaterialTheme.colorScheme.error,
        )
      }
    } else {
      null
    },
    trailingContent = {
      Box {
        if (!enabled) {
          val tentPainter = rememberTentVectorPainter()
          Image(
            tentPainter,
            contentDescription = null,
            modifier = Modifier
              .clickable(enabled = enabled) {
                isExpanded = true
              }
              .size(TentIconSize),
          )
        } else {
          Image(
            tent.icon,
            contentDescription = null,
            modifier = Modifier
              .clickable(enabled = enabled) {
                isExpanded = true
              }
              .size(TentIconSize),
          )
        }

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
      .clickable(enabled = enabled) { isExpanded = true },
  )
}
