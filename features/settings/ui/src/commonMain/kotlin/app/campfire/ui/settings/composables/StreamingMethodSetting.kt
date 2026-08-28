// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.AutoMode
import app.campfire.common.compose.icons.rounded.PlayArrow
import app.campfire.common.compose.icons.rounded.Sensors
import app.campfire.settings.api.StreamingMethod
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.setting_streaming_method_subtitle
import campfire.features.settings.ui.generated.resources.setting_streaming_method_title
import campfire.features.settings.ui.generated.resources.streaming_method_auto
import campfire.features.settings.ui.generated.resources.streaming_method_auto_description
import campfire.features.settings.ui.generated.resources.streaming_method_direct_play
import campfire.features.settings.ui.generated.resources.streaming_method_direct_play_description
import campfire.features.settings.ui.generated.resources.streaming_method_prefer_hls
import campfire.features.settings.ui.generated.resources.streaming_method_prefer_hls_description
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun StreamingMethodSetting(
  method: StreamingMethod,
  onMethodChange: (StreamingMethod) -> Unit,
  modifier: Modifier = Modifier,
) {
  var isExpanded by remember { mutableStateOf(false) }
  SettingListItem(
    modifier = modifier.clickable { isExpanded = true },
    headlineContent = { Text(stringResource(Res.string.setting_streaming_method_title)) },
    supportingContent = { Text(stringResource(Res.string.setting_streaming_method_subtitle)) },
    trailingContent = {
      Box {
        StreamingMethodChip(
          method = method,
          onClick = { isExpanded = true },
        )

        DropdownMenu(
          expanded = isExpanded,
          onDismissRequest = { isExpanded = false },
        ) {
          StreamingMethod.entries.forEach { m ->
            DropdownMenuItem(
              text = {
                Column(
                  modifier = Modifier
                    .widthIn(max = 240.dp)
                    .padding(
                      vertical = 8.dp,
                    ),
                ) {
                  Text(
                    text = stringResource(m.label),
                    style = MaterialTheme.typography.titleSmall,
                  )
                  Text(
                    text = stringResource(m.description),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              },
              leadingIcon = {
                Icon(
                  m.icon,
                  contentDescription = null,
                )
              },
              onClick = {
                onMethodChange(m)
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
private fun StreamingMethodChip(
  method: StreamingMethod,
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
        method.icon,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Spacer(Modifier.width(8.dp))
      Text(
        text = stringResource(method.label),
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

private val StreamingMethod.label: StringResource
  get() = when (this) {
    StreamingMethod.AUTO -> Res.string.streaming_method_auto
    StreamingMethod.DIRECT_PLAY_ONLY -> Res.string.streaming_method_direct_play
    StreamingMethod.PREFER_HLS -> Res.string.streaming_method_prefer_hls
  }

private val StreamingMethod.description: StringResource
  get() = when (this) {
    StreamingMethod.AUTO -> Res.string.streaming_method_auto_description
    StreamingMethod.DIRECT_PLAY_ONLY -> Res.string.streaming_method_direct_play_description
    StreamingMethod.PREFER_HLS -> Res.string.streaming_method_prefer_hls_description
  }

private val StreamingMethod.icon: ImageVector
  get() = when (this) {
    StreamingMethod.AUTO -> CampfireIcons.Rounded.AutoMode
    StreamingMethod.DIRECT_PLAY_ONLY -> CampfireIcons.Rounded.PlayArrow
    StreamingMethod.PREFER_HLS -> CampfireIcons.Rounded.Sensors
  }
