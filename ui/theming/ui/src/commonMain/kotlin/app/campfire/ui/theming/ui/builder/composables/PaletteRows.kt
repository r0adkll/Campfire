// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Replay
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.ui.theming.ui.builder.PaletteKey
import app.campfire.ui.theming.ui.builder.PaletteRow
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.theme_palette_custom
import campfire.ui.theming.ui.generated.resources.theme_palette_generated
import campfire.ui.theming.ui.generated.resources.theme_palette_reset
import campfire.ui.theming.ui.generated.resources.theme_palette_your_color
import org.jetbrains.compose.resources.stringResource

/** One row per palette, showing its tones; tapping a row picks that palette's color. */
@Composable
internal fun PaletteRows(
  palettes: List<PaletteRow>,
  onPaletteClick: (PaletteKey) -> Unit,
  onResetClick: (PaletteKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    palettes.forEach { row ->
      PaletteRowItem(
        row = row,
        onClick = { onPaletteClick(row.key) },
        onResetClick = { onResetClick(row.key) },
      )
    }
  }
}

@Composable
private fun PaletteRowItem(
  row: PaletteRow,
  onClick: () -> Unit,
  onResetClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(MaterialTheme.shapes.medium)
      .clickable(onClick = onClick)
      .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Column(Modifier.width(112.dp)) {
      Text(row.key.label(), style = MaterialTheme.typography.labelLarge)
      Text(
        text = stringResource(
          when {
            row.key == PaletteKey.Accent -> Res.string.theme_palette_your_color
            row.isCustom -> Res.string.theme_palette_custom
            else -> Res.string.theme_palette_generated
          },
        ),
        style = MaterialTheme.typography.bodySmall,
        color = if (row.isCustom) colors.primary else colors.onSurfaceVariant,
      )
    }

    Row(
      Modifier
        .weight(1f)
        .height(28.dp)
        .clip(RoundedCornerShape(8.dp)),
    ) {
      row.tones.forEach { tone ->
        Box(
          Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(tone),
        )
      }
    }

    if (row.isCustom) {
      val resetLabel = stringResource(Res.string.theme_palette_reset)
      IconButtonTooltip(text = resetLabel) {
        IconButton(onClick = onResetClick) {
          Icon(CampfireIcons.Rounded.Replay, contentDescription = resetLabel)
        }
      }
    } else {
      Box(Modifier.size(48.dp))
    }
  }
}
