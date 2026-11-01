// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.BrightnessHigh
import app.campfire.common.compose.icons.rounded.BrightnessLow
import app.campfire.common.compose.icons.rounded.BrightnessMedium
import app.campfire.ui.theming.ui.builder.ContrastLevel

/**
 * The three contrast levels as a connected button group. Below [IconsMinWidth] the buttons drop
 * their icons, so a phone's tray or a narrow column fits the whole of each label.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ContrastPicker(
  level: ContrastLevel,
  onLevelClick: (ContrastLevel) -> Unit,
  modifier: Modifier = Modifier,
) = BoxWithConstraints(modifier) {
  val showIcons = maxWidth >= IconsMinWidth
  Row(
    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
  ) {
    val modifiers = listOf(Modifier.weight(1f), Modifier.weight(1f), Modifier.weight(1f))
    ContrastLevel.entries.forEachIndexed { index, contrastLevel ->
      OutlinedToggleButton(
        checked = contrastLevel == level,
        onCheckedChange = {
          onLevelClick(contrastLevel)
        },
        shapes =
        when (index) {
          0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
          ContrastLevel.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
          else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
        },
        colors = ToggleButtonDefaults.outlinedToggleButtonColors(
          checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
          checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        contentPadding = PaddingValues(horizontal = 8.dp),
        modifier = modifiers[index],
      ) {
        if (showIcons) {
          Icon(
            when (contrastLevel) {
              ContrastLevel.Normal -> CampfireIcons.Rounded.BrightnessLow
              ContrastLevel.Medium -> CampfireIcons.Rounded.BrightnessMedium
              ContrastLevel.High -> CampfireIcons.Rounded.BrightnessHigh
            },
            contentDescription = null,
            modifier = Modifier.size(ToggleButtonDefaults.IconSize),
          )
          Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
        }
        Text(contrastLevel.label(), maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    }
  }
}

/** Wide enough for each button's icon and the whole of its label. */
private val IconsMinWidth = 352.dp
