// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.asHct
import app.campfire.ui.theming.ui.builder.PaletteKey
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.theme_picker_colorfulness
import campfire.ui.theming.ui.generated.resources.theme_picker_done
import campfire.ui.theming.ui.generated.resources.theme_picker_hex
import campfire.ui.theming.ui.generated.resources.theme_picker_hue
import campfire.ui.theming.ui.generated.resources.theme_picker_note
import campfire.ui.theming.ui.generated.resources.theme_picker_suggestions
import campfire.ui.theming.ui.generated.resources.theme_picker_use_generated
import com.r0adkll.swatchbuckler.color.hct.Hct
import kotlin.math.max
import org.jetbrains.compose.resources.stringResource

private const val MAX_HUE = 360f
private const val MAX_CHROMA = 120f

/**
 * Tones near the middle keep the most chroma available, so the colorfulness slider stays useful.
 * The scheme picks each role's tone anyway, so the tone of a palette pick barely matters.
 */
private const val MIN_TONE = 35.0
private const val MAX_TONE = 70.0

/**
 * Picks a palette color with hue and colorfulness sliders, a hex field and swatches. Every change
 * is reported right away so the preview follows along. Lightness is left out on purpose: the
 * scheme sets each role's lightness per mode, so a lightness slider would barely change anything.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ColorPickerPanel(
  key: PaletteKey,
  initialColor: Color,
  seedColor: Color,
  canReset: Boolean,
  onColorChange: (Color) -> Unit,
  onResetClick: () -> Unit,
  onDoneClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val start = remember { initialColor.asHct() }
  val tone = remember { start.tone.coerceIn(MIN_TONE, MAX_TONE) }
  var hue by remember { mutableFloatStateOf(start.hue.toFloat()) }
  var chroma by remember { mutableFloatStateOf(start.chroma.toFloat()) }
  var current by remember { mutableStateOf(initialColor) }
  val hex = rememberTextFieldState(initialColor.hexCode().drop(1))
  val suggestions = remember { harmonies(seedColor) }

  fun pick(color: Color, updateHex: Boolean = true) {
    current = color
    if (updateHex) hex.setTextAndPlaceCursorAtEnd(color.hexCode().drop(1))
    onColorChange(color)
  }

  fun pickExact(color: Color, updateHex: Boolean = true) {
    val hct = color.asHct()
    hue = hct.hue.toFloat()
    chroma = hct.chroma.toFloat()
    pick(color, updateHex)
  }

  LaunchedEffect(hex) {
    snapshotFlow { hex.text.toString() }.collect { text ->
      val typed = text.parseHexColor() ?: return@collect
      if (typed != current) pickExact(typed, updateHex = false)
    }
  }

  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      ColorDot(current, size = 40.dp)
      Text(
        text = key.label(),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier
          .weight(1f)
          .padding(start = 12.dp),
      )
      FilledTonalButton(onClick = onDoneClick) {
        Text(stringResource(Res.string.theme_picker_done))
      }
    }

    Text(
      text = stringResource(Res.string.theme_picker_note),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    GradientSlider(
      label = stringResource(Res.string.theme_picker_hue),
      value = hue,
      maxValue = MAX_HUE,
      trackColors = List(13) { colorOf(it * 30.0, max(chroma.toDouble(), 30.0), tone) },
      onValueChange = {
        hue = it
        pick(colorOf(hue.toDouble(), chroma.toDouble(), tone))
      },
    )
    GradientSlider(
      label = stringResource(Res.string.theme_picker_colorfulness),
      value = chroma,
      maxValue = MAX_CHROMA,
      trackColors = List(7) { colorOf(hue.toDouble(), it * 20.0, tone) },
      onValueChange = {
        chroma = it
        pick(colorOf(hue.toDouble(), chroma.toDouble(), tone))
      },
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
      OutlinedTextField(
        state = hex,
        label = { Text(stringResource(Res.string.theme_picker_hex)) },
        prefix = { Text("#") },
        lineLimits = TextFieldLineLimits.SingleLine,
        inputTransformation = InputTransformation.maxLength(6),
        modifier = Modifier.width(160.dp),
      )
      Spacer(Modifier.weight(1f))
      if (canReset) {
        TextButton(onClick = onResetClick) {
          Text(stringResource(Res.string.theme_picker_use_generated))
        }
      }
    }

    Text(
      text = stringResource(Res.string.theme_picker_suggestions),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SwatchRow(
      colors = suggestions + CuratedColors,
      selected = current,
      onSwatchClick = { pickExact(it) },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GradientSlider(
  label: String,
  value: Float,
  maxValue: Float,
  trackColors: List<Color>,
  onValueChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  val thumbColor = MaterialTheme.colorScheme.onSurface
  val thumbBorder = MaterialTheme.colorScheme.surfaceContainer
  Column(modifier) {
    Text(label, style = MaterialTheme.typography.labelLarge)
    Slider(
      value = value,
      onValueChange = onValueChange,
      valueRange = 0f..maxValue,
      thumb = {
        Box(
          Modifier
            .size(width = 8.dp, height = 32.dp)
            .background(thumbColor, CircleShape)
            .border(2.dp, thumbBorder, CircleShape),
        )
      },
      track = {
        Box(
          Modifier
            .fillMaxWidth()
            .height(20.dp)
            .clip(CircleShape)
            .background(Brush.horizontalGradient(trackColors)),
        )
      },
      modifier = Modifier
        .fillMaxWidth()
        .semantics { contentDescription = label },
    )
  }
}

internal fun colorOf(hue: Double, chroma: Double, tone: Double): Color = Color(Hct.from(hue, chroma, tone).toInt())

/** Colors that sit well next to [seed]: its neighbors, a triad and its complement. */
private fun harmonies(seed: Color): List<Color> {
  val hct = seed.asHct()
  val chroma = max(hct.chroma, 36.0)
  return listOf(30.0, -30.0, 120.0, 180.0).map { offset ->
    colorOf((hct.hue + offset + 360.0) % 360.0, chroma, 50.0)
  }
}

/** Six hex digits, without the hash, as an opaque color; anything else is null. */
internal fun String.parseHexColor(): Color? {
  if (length != 6) return null
  val rgb = toLongOrNull(16) ?: return null
  return Color(0xFF000000L or rgb)
}
