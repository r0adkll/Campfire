// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.stats.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.DarkMode
import app.campfire.common.compose.icons.rounded.LightMode
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.compose.widgets.NavigationBackButton
import campfire.features.stats.ui.generated.resources.Res
import campfire.features.stats.ui.generated.resources.action_toggle_dark_mode
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PreviewScaffold(
  useDarkColors: Boolean = false,
  content: @Composable ColumnScope.() -> Unit,
) {
  var _useDarkColors by remember { mutableStateOf(useDarkColors) }

  CampfireTheme(
    useDarkColors = _useDarkColors,
  ) {
    Scaffold(
      topBar = {
        CompositionLocalProvider(
          LocalContentLayout provides ContentLayout.Root,
        ) {
          CampfireTopAppBar(
            title = { Text("User statistics") },
            navigationIcon = {
              NavigationBackButton(onClick = {})
            },
            actions = {
              val toggleDarkLabel = stringResource(Res.string.action_toggle_dark_mode)
              IconButtonTooltip(text = toggleDarkLabel) {
                IconButton(
                  onClick = { _useDarkColors = !_useDarkColors },
                ) {
                  Icon(
                    if (_useDarkColors) CampfireIcons.Rounded.LightMode else CampfireIcons.Rounded.DarkMode,
                    contentDescription = toggleDarkLabel,
                  )
                }
              }
            },
          )
        }
      },
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(top = 56.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
      )
    }
  }
}
