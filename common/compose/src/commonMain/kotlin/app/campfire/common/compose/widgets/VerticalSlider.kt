// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * A vertically oriented Material 3 [Slider], drag up to increase. The caller's [modifier]
 * sizes the vertical footprint (e.g. `Modifier.height(160.dp)`); the underlying slider is
 * rotated into it so Material semantics, keyboard, and accessibility behavior are preserved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerticalSlider(
  value: Float,
  onValueChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
  onValueChangeFinished: (() -> Unit)? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }
  Slider(
    value = value,
    onValueChange = onValueChange,
    onValueChangeFinished = onValueChangeFinished,
    valueRange = valueRange,
    enabled = enabled,
    interactionSource = interactionSource,
    thumb = {
      SliderDefaults.Thumb(
        interactionSource = interactionSource,
        enabled = enabled,
        thumbSize = DpSize(4.dp, 32.dp),
      )
    },
    modifier = modifier
      .graphicsLayer { rotationZ = 270f }
      .layout { measurable, constraints ->
        // Swap the incoming constraints so the rotated slider measures its length against
        // the caller's height, then report the swapped size back
        val placeable = measurable.measure(
          Constraints(
            minWidth = constraints.minHeight,
            maxWidth = constraints.maxHeight,
            minHeight = constraints.minWidth,
            maxHeight = constraints.maxWidth,
          ),
        )
        layout(placeable.height, placeable.width) {
          placeable.place(
            x = -(placeable.width - placeable.height) / 2,
            y = (placeable.width - placeable.height) / 2,
          )
        }
      },
  )
}
