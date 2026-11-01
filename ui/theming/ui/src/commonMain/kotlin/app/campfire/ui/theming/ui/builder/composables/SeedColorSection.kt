// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.ui.theming.ui.builder.CoverColor
import app.campfire.ui.theming.ui.preview.PreviewCover
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.theme_builder_color_change
import campfire.ui.theming.ui.generated.resources.theme_builder_from_your_books
import campfire.ui.theming.ui.generated.resources.theme_builder_use_color
import campfire.ui.theming.ui.generated.resources.theme_builder_use_cover_color
import org.jetbrains.compose.resources.stringResource

/** Colors that make good seeds: warm to cool, plus a stone grey. */
internal val CuratedColors = listOf(
  Color(0xFF904B3C),
  Color(0xFFC0532D),
  Color(0xFFD97706),
  Color(0xFFCA8A04),
  Color(0xFF4D7C0F),
  Color(0xFF0F766E),
  Color(0xFF0E7490),
  Color(0xFF2563EB),
  Color(0xFF7C3AED),
  Color(0xFFBE185D),
  Color(0xFF57534E),
)

/**
 * The seed color with a tap target for the picker, and quick swatches beneath it: curated colors,
 * then the colors of the user's own books, once their covers have been read.
 */
@Composable
internal fun SeedColorSection(
  seedColor: Color,
  onChangeClick: () -> Unit,
  onSwatchClick: (Color) -> Unit,
  modifier: Modifier = Modifier,
  coverColors: List<CoverColor> = emptyList(),
) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Surface(
      onClick = onChangeClick,
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        ColorDot(seedColor, size = 44.dp)
        Column {
          Text(seedColor.hexCode(), style = MaterialTheme.typography.titleMedium)
          Text(
            text = stringResource(Res.string.theme_builder_color_change),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    SwatchRow(
      colors = CuratedColors,
      selected = seedColor,
      onSwatchClick = onSwatchClick,
    )

    if (coverColors.isNotEmpty()) {
      Text(
        text = stringResource(Res.string.theme_builder_from_your_books),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(
        modifier = Modifier
          .horizontalScroll(rememberScrollState())
          .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        coverColors.forEach { coverColor ->
          CoverSwatch(
            coverColor = coverColor,
            selected = coverColor.color == seedColor,
            onClick = { onSwatchClick(coverColor.color) },
          )
        }
      }
    }
  }
}

/**
 * [SeedColorSection] for the tool tray, in two rows: the seed color beside the curated swatches,
 * which scroll sideways, then the colors of the user's books.
 */
@Composable
internal fun CompactSeedColorSection(
  seedColor: Color,
  onChangeClick: () -> Unit,
  onSwatchClick: (Color) -> Unit,
  modifier: Modifier = Modifier,
  coverColors: List<CoverColor> = emptyList(),
) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Surface(
        onClick = onChangeClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
      ) {
        Row(
          modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          ColorDot(seedColor, size = 36.dp)
          Column {
            Text(seedColor.hexCode(), style = MaterialTheme.typography.titleSmall)
            Text(
              text = stringResource(Res.string.theme_builder_color_change),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
      Row(
        modifier = Modifier
          .weight(1f)
          .horizontalScroll(rememberScrollState())
          .selectableGroup()
          .padding(start = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        CuratedColors.forEach { color ->
          Swatch(color = color, selected = color == seedColor, onClick = { onSwatchClick(color) })
        }
      }
    }

    if (coverColors.isNotEmpty()) {
      Text(
        text = stringResource(Res.string.theme_builder_from_your_books),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(
        modifier = Modifier
          .horizontalScroll(rememberScrollState())
          .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        coverColors.forEach { coverColor ->
          CoverSwatch(
            coverColor = coverColor,
            selected = coverColor.color == seedColor,
            onClick = { onSwatchClick(coverColor.color) },
          )
        }
      }
    }
  }
}

/** A book's cover, with a dot of the color it gives the theme. */
@Composable
private fun CoverSwatch(
  coverColor: CoverColor,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val label = stringResource(Res.string.theme_builder_use_cover_color, coverColor.item.media.metadata.title.orEmpty())
  val shape = RoundedCornerShape(12.dp)
  Box(
    modifier = modifier
      .size(56.dp)
      .clip(shape)
      .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
      .semantics { contentDescription = label }
      .then(if (selected) Modifier.border(2.dp, colors.onSurface, shape) else Modifier)
      .padding(if (selected) 4.dp else 0.dp),
  ) {
    PreviewCover(
      imageUrl = coverColor.item.media.coverImageUrl,
      size = 56.dp,
      shape = RoundedCornerShape(if (selected) 8.dp else 12.dp),
      modifier = Modifier.matchParentSize(),
    )
    ColorDot(
      color = coverColor.color,
      size = 20.dp,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(2.dp)
        .border(2.dp, colors.surfaceContainer, CircleShape),
    )
  }
}

@Composable
internal fun SwatchRow(
  colors: List<Color>,
  selected: Color?,
  onSwatchClick: (Color) -> Unit,
  modifier: Modifier = Modifier,
) {
  FlowRow(
    modifier = modifier.selectableGroup(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    colors.forEach { color ->
      Swatch(color = color, selected = color == selected, onClick = { onSwatchClick(color) })
    }
  }
}

@Composable
private fun Swatch(
  color: Color,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val label = stringResource(Res.string.theme_builder_use_color, color.hexCode())
  ColorDot(
    color = color,
    size = 36.dp,
    selected = selected,
    modifier = modifier
      .clip(CircleShape)
      .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
      .semantics { contentDescription = label },
  )
}

@Composable
internal fun ColorDot(
  color: Color,
  size: Dp,
  modifier: Modifier = Modifier,
  selected: Boolean = false,
) {
  val ring = MaterialTheme.colorScheme.onSurface
  Box(
    modifier = modifier
      .size(size)
      .then(
        if (selected) {
          Modifier.border(2.dp, ring, CircleShape).padding(4.dp)
        } else {
          Modifier
        },
      )
      .background(color, CircleShape)
      .border(1.dp, ring.copy(alpha = 0.16f), CircleShape),
  )
}
