package app.campfire.ui.settings.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
internal fun <T> DropdownSetting(
  value: T,
  values: List<T>,
  onValueChange: (T) -> Unit,
  headlineContent: @Composable () -> Unit,
  supportingContent: @Composable () -> Unit,
  itemIcon: @Composable (T) -> Unit,
  itemText: @Composable (T) -> Unit,
  modifier: Modifier = Modifier,
) {
  var isExpanded by remember { mutableStateOf(false) }
  SettingListItem(
    modifier = modifier.clickable { isExpanded = true },
    headlineContent = headlineContent,
    supportingContent = supportingContent,
    trailingContent = {
      Box {
        OptionChip(
          icon = { itemIcon(value) },
          text = { itemText(value) },
          onClick = { isExpanded = true },
        )

        DropdownMenu(
          expanded = isExpanded,
          onDismissRequest = { isExpanded = false },
        ) {
          values.forEach { option ->
            DropdownMenuItem(
              leadingIcon = { itemIcon(option) },
              text = { itemText(option) },
              onClick = {
                onValueChange(option)
                isExpanded = false
              },
            )
          }
        }
      }
    },
  )
}

@Composable
private fun OptionChip(
  icon: @Composable () -> Unit,
  text: @Composable () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .background(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(8.dp),
      )
      .border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(8.dp),
      )
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
      .padding(
        horizontal = 16.dp,
        vertical = 8.dp,
      ),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    CompositionLocalProvider(
      LocalContentColor provides MaterialTheme.colorScheme.primary,
    ) {
      icon()
      Spacer(Modifier.width(8.dp))
      ProvideTextStyle(MaterialTheme.typography.titleSmall) {
        text()
      }
      Spacer(Modifier.width(4.dp))
      Icon(
        Icons.Rounded.ArrowDropDown,
        contentDescription = null,
      )
    }
  }
}
