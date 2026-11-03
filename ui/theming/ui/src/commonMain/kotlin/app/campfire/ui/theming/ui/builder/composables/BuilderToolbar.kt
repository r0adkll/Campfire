// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.FormatPaint
import app.campfire.common.compose.icons.rounded.Palette
import app.campfire.common.compose.icons.rounded.Tune
import app.campfire.common.compose.icons.rounded.WaterDrop
import app.campfire.ui.theming.ui.builder.BuilderTool
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.theme_builder_section_fine_tune
import campfire.ui.theming.ui.generated.resources.theme_builder_section_style
import campfire.ui.theming.ui.generated.resources.theme_builder_tool_color
import campfire.ui.theming.ui.generated.resources.theme_builder_tool_palette
import campfire.ui.theming.ui.generated.resources.theme_builder_tools
import org.jetbrains.compose.resources.stringResource

/**
 * The floating bar under the tray that picks which [BuilderTool] the tray shows. [selected] is
 * null while the tray is hidden, so no tool reads as open.
 */
@Composable
internal fun BuilderToolbar(
  selected: BuilderTool?,
  onToolClick: (BuilderTool) -> Unit,
  modifier: Modifier = Modifier,
) {
  val groupLabel = stringResource(Res.string.theme_builder_tools)
  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier
        .padding(6.dp)
        .selectableGroup()
        .semantics { contentDescription = groupLabel },
      horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      BuilderTool.entries.forEach { tool ->
        ToolButton(
          label = tool.label(),
          icon = tool.icon,
          selected = tool == selected,
          onClick = { onToolClick(tool) },
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun ToolButton(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Column(
    modifier = modifier
      .height(48.dp)
      .clip(CircleShape)
      .background(if (selected) colors.secondaryContainer else Color.Transparent)
      .selectable(selected = selected, role = Role.Tab, onClick = onClick),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
      modifier = Modifier.size(20.dp),
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

private val BuilderTool.icon: ImageVector
  get() = when (this) {
    BuilderTool.Color -> CampfireIcons.Rounded.WaterDrop
    BuilderTool.Style -> CampfireIcons.Rounded.FormatPaint
    BuilderTool.Palette -> CampfireIcons.Rounded.Palette
    BuilderTool.FineTune -> CampfireIcons.Rounded.Tune
  }

@Composable
private fun BuilderTool.label(): String = stringResource(
  when (this) {
    BuilderTool.Color -> Res.string.theme_builder_tool_color
    BuilderTool.Style -> Res.string.theme_builder_section_style
    BuilderTool.Palette -> Res.string.theme_builder_tool_palette
    BuilderTool.FineTune -> Res.string.theme_builder_section_fine_tune
  },
)
