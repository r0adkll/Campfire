// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Replay
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.ui.theming.ui.builder.PaletteKey
import app.campfire.ui.theming.ui.builder.PaletteRow
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.theme_palette_custom
import campfire.ui.theming.ui.generated.resources.theme_palette_generated
import campfire.ui.theming.ui.generated.resources.theme_palette_hint
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

/**
 * [PaletteRows] for the tool tray: the palettes side by side as tiles of their tones, a ring
 * marking the ones set by hand. Tapping a tile picks that palette's color. The tiles share the
 * width, but never get narrower than [MinPaletteTileWidth]: on a phone the row scrolls sideways
 * rather than breaking "Backgrounds" in two.
 */
@Composable
internal fun PaletteTiles(
  palettes: List<PaletteRow>,
  onPaletteClick: (PaletteKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = stringResource(Res.string.theme_palette_hint),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    BoxWithConstraints(Modifier.fillMaxWidth()) {
      val gaps = PaletteTileGap * (palettes.size - 1)
      val tileWidth = maxOf(MinPaletteTileWidth, (maxWidth - gaps) / palettes.size.coerceAtLeast(1))
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(PaletteTileGap),
      ) {
        palettes.forEach { row ->
          PaletteTile(row = row, onClick = { onPaletteClick(row.key) }, modifier = Modifier.width(tileWidth))
        }
      }
    }
  }
}

/** Wide enough for the longest palette name, "Backgrounds", on one line. */
private val MinPaletteTileWidth = 84.dp
private val PaletteTileGap = 8.dp

@Composable
private fun PaletteTile(
  row: PaletteRow,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val shape = RoundedCornerShape(12.dp)
  val custom = stringResource(Res.string.theme_palette_custom)
  Column(
    modifier = modifier
      .clip(MaterialTheme.shapes.medium)
      .clickable(role = Role.Button, onClick = onClick)
      .semantics { if (row.isCustom) stateDescription = custom }
      .padding(vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(
      Modifier
        .fillMaxWidth()
        .height(44.dp)
        .then(if (row.isCustom) Modifier.border(2.dp, colors.primary, shape).padding(4.dp) else Modifier)
        .clip(if (row.isCustom) RoundedCornerShape(8.dp) else shape),
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
    Text(
      text = row.key.label(),
      style = MaterialTheme.typography.labelMedium,
      textAlign = TextAlign.Center,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
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
