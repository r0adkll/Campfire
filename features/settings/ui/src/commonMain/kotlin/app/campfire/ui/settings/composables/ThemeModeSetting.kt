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
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import app.campfire.core.extensions.capitalized
import app.campfire.settings.api.ThemeMode
import app.campfire.settings.api.ThemeMode.DARK
import app.campfire.settings.api.ThemeMode.LIGHT
import app.campfire.settings.api.ThemeMode.SYSTEM
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.setting_theme_description
import campfire.features.settings.ui.generated.resources.setting_theme_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ThemeModeSetting(
  themeMode: ThemeMode,
  onThemeChange: (ThemeMode) -> Unit,
  modifier: Modifier = Modifier,
) {
  var isExpanded by remember { mutableStateOf(false) }
  SettingListItem(
    modifier = modifier.clickable { isExpanded = true },
    headlineContent = { Text(stringResource(Res.string.setting_theme_title)) },
    supportingContent = { Text(stringResource(Res.string.setting_theme_description)) },
    trailingContent = {
      Box {
        ThemeModeChip(
          themeMode = themeMode,
          onClick = { isExpanded = true },
        )

        DropdownMenu(
          expanded = isExpanded,
          onDismissRequest = { isExpanded = false },
        ) {
          ThemeMode.entries.forEach { t ->
            DropdownMenuItem(
              leadingIcon = {
                Icon(
                  when (t) {
                    LIGHT -> Icons.Rounded.LightMode
                    DARK -> Icons.Rounded.DarkMode
                    SYSTEM -> Icons.Rounded.Brightness6
                  },
                  contentDescription = null,
                )
              },
              text = { Text(t.name.lowercase().capitalized()) },
              onClick = {
                onThemeChange(t)
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
private fun ThemeModeChip(
  themeMode: ThemeMode,
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
      Icon(
        when (themeMode) {
          LIGHT -> Icons.Rounded.LightMode
          DARK -> Icons.Rounded.DarkMode
          SYSTEM -> Icons.Rounded.Brightness6
        },
        contentDescription = null,
      )
      Spacer(Modifier.width(8.dp))
      Text(
        text = themeMode.name.lowercase().capitalized(),
        style = MaterialTheme.typography.titleSmall,
      )
      Spacer(Modifier.width(4.dp))
      Icon(
        Icons.Rounded.ArrowDropDown,
        contentDescription = null,
      )
    }
  }
}
