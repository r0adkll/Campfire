package app.campfire.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.isSupportingPaneEnabled
import app.campfire.common.screens.SettingsScreen
import app.campfire.common.settings.CampfireSettings
import app.campfire.common.settings.CampfireSettings.Theme.DARK
import app.campfire.common.settings.CampfireSettings.Theme.LIGHT
import app.campfire.common.settings.CampfireSettings.Theme.SYSTEM
import app.campfire.core.Platform
import app.campfire.core.currentPlatform
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.capitalized
import app.campfire.ui.settings.composables.Header
import app.campfire.ui.settings.composables.TentSetting
import campfire.ui.settings.generated.resources.Res
import campfire.ui.settings.generated.resources.setting_dynamic_colors_description
import campfire.ui.settings.generated.resources.setting_dynamic_colors_title
import campfire.ui.settings.generated.resources.setting_theme_description
import campfire.ui.settings.generated.resources.setting_theme_title
import campfire.ui.settings.generated.resources.setting_version_title
import campfire.ui.settings.generated.resources.settings_title
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@CircuitInject(SettingsScreen::class, UserScope::class)
@Composable
fun Settings(
  state: SettingsUiState,
  modifier: Modifier = Modifier,
) {
  val windowSizeClass by rememberUpdatedState(LocalWindowSizeClass.current)

  Scaffold(
    topBar = {
      if (!windowSizeClass.isSupportingPaneEnabled) {
        TopAppBar(
          title = { Text(stringResource(Res.string.settings_title)) },
          navigationIcon = {
            IconButton(
              onClick = { state.eventSink(SettingsUiEvent.Back) },
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = null,
              )
            }
          },
        )
      }
    },
    modifier = modifier,
    contentWindowInsets = CampfireWindowInsets,
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .verticalScroll(rememberScrollState())
        .padding(paddingValues),
    ) {
      Header(
        title = {
          Text("Theme & Style")
        },
      )

      TentSetting(
        tent = state.tent,
        onTentChange = { state.eventSink(SettingsUiEvent.ChangeTent(it)) },
      )

      ListItem(
        headlineContent = { Text(stringResource(Res.string.setting_theme_title)) },
        supportingContent = { Text(stringResource(Res.string.setting_theme_description)) },
        trailingContent = {
          var isExpanded by remember { mutableStateOf(false) }
          Box {
            Row(
              modifier = Modifier
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
                .clickable { isExpanded = true }
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
                  when (state.theme) {
                    LIGHT -> Icons.Rounded.LightMode
                    DARK -> Icons.Rounded.DarkMode
                    SYSTEM -> Icons.Rounded.Brightness6
                  },
                  contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                  text = state.theme.name.lowercase().capitalized(),
                  style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                  Icons.Rounded.ArrowDropDown,
                  contentDescription = null,
                )
              }
            }

            DropdownMenu(
              expanded = isExpanded,
              onDismissRequest = { isExpanded = false },
            ) {
              CampfireSettings.Theme.entries.forEach { t ->
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
                    state.eventSink(SettingsUiEvent.Theme(t))
                    isExpanded = false
                  },
                )
              }
            }
          }
        },
      )

      if (currentPlatform == Platform.ANDROID) {
        ListItem(
          headlineContent = { Text(stringResource(Res.string.setting_dynamic_colors_title)) },
          supportingContent = { Text(stringResource(Res.string.setting_dynamic_colors_description)) },
          trailingContent = {
            Switch(
              checked = state.useDynamicColors,
              onCheckedChange = { state.eventSink(SettingsUiEvent.UseDynamicColors(it)) },
            )
          },
        )
      }

      Header(
        title = {
          Text("About")
        },
      )

      ListItem(
        headlineContent = { Text(stringResource(Res.string.setting_version_title)) },
        supportingContent = { Text(state.applicationInfo.versionName) },
      )
    }
  }
}
