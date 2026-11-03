// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.campfire.ui.theming.ui.builder.StyleOption
import app.campfire.ui.theming.ui.builder.StyleSwatch
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.theme_builder_style_classic
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import org.jetbrains.compose.resources.stringResource

/** A scrolling row of style cards, each drawn with the colors that style would give. */
@Composable
internal fun StylePicker(
  styles: List<StyleOption>,
  selected: Variant,
  isDarkMode: Boolean,
  onStyleClick: (Variant) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .height(IntrinsicSize.Max)
      .horizontalScroll(rememberScrollState())
      .selectableGroup(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    styles.forEach { option ->
      StyleCard(
        option = option,
        swatch = if (isDarkMode) option.dark else option.light,
        selected = option.style == selected,
        onClick = { onStyleClick(option.style) },
      )
    }
  }
}

@Composable
private fun StyleCard(
  option: StyleOption,
  swatch: StyleSwatch,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val shape = MaterialTheme.shapes.large
  Column(
    modifier = modifier
      .width(148.dp)
      .fillMaxHeight()
      .clip(shape)
      .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
      .background(if (selected) colors.secondaryContainer else colors.surfaceContainerHigh)
      .then(if (selected) Modifier.border(2.dp, colors.primary, shape) else Modifier)
      .padding(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    SwatchThumbnail(swatch, Modifier.fillMaxWidth())
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = option.style.label(),
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) colors.onSecondaryContainer else colors.onSurface,
      )
      if (option.isClassic) {
        Text(
          text = stringResource(Res.string.theme_builder_style_classic),
          style = MaterialTheme.typography.labelSmall,
          color = colors.onSurfaceVariant,
          modifier = Modifier
            .background(colors.surfaceContainerHighest, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp),
        )
      }
    }
    Text(
      text = option.style.description(),
      style = MaterialTheme.typography.bodySmall,
      color = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
    )
  }
}

/** The style's accent, selection and third accent colors on its card surface. */
@Composable
private fun SwatchThumbnail(
  swatch: StyleSwatch,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .height(52.dp)
      .background(swatch.surface, RoundedCornerShape(10.dp))
      .padding(8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Box(
      Modifier
        .size(32.dp)
        .background(swatch.primary, CircleShape),
    )
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Box(
        Modifier
          .fillMaxWidth()
          .height(14.dp)
          .background(swatch.secondaryContainer, RoundedCornerShape(7.dp)),
      )
      Box(
        Modifier
          .fillMaxWidth(0.6f)
          .height(14.dp)
          .background(swatch.tertiary, RoundedCornerShape(7.dp)),
      )
    }
  }
}
