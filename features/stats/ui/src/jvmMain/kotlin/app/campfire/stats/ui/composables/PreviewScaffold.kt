package app.campfire.stats.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
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
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.core.model.Tent

@Composable
internal fun PreviewScaffold(
  tent: Tent = Tent.Default,
  useDarkColors: Boolean = false,
  content: @Composable ColumnScope.() -> Unit,
) {
  var _useDarkColors by remember { mutableStateOf(useDarkColors) }

  CampfireTheme(
    tent = tent,
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
              IconButton(
                onClick = {},
              ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
              }
            },
            actions = {
              IconButton(
                onClick = { _useDarkColors = !_useDarkColors },
              ) {
                Icon(
                  if (_useDarkColors) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                  contentDescription = null,
                )
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
