// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.PlaybackSpeedDialog
import app.campfire.common.compose.widgets.PlaybackSpeedRange
import app.campfire.core.extensions.readableHundredths
import app.campfire.core.extensions.roundToHundredths
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.setting_playback_speed_option_edit
import org.jetbrains.compose.resources.stringResource

/**
 * Editors for the quick playback speed options offered in the playback speed sheet — one
 * slider row per option, with the value label clickable to type an exact speed.
 */
@Composable
internal fun PlaybackSpeedOptionsSetting(
  rates: List<Float>,
  onRateChange: (index: Int, rate: Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(4.dp),
    modifier = modifier.padding(horizontal = 16.dp),
  ) {
    rates.forEachIndexed { index, rate ->
      SpeedOptionRow(
        rate = rate,
        shape = rowShape(index = index, total = rates.size),
        onRateChange = { onRateChange(index, it) },
      )
    }
  }
}

@Composable
private fun SpeedOptionRow(
  rate: Float,
  shape: Shape,
  onRateChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(
    shape = shape,
    modifier = modifier,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
      var sliderValue by remember { mutableStateOf(rate) }

      LaunchedEffect(rate) {
        if (sliderValue != rate) {
          sliderValue = rate
        }
      }

      val setRate: (Float) -> Unit = { raw ->
        val speed = raw.roundToHundredths().coerceIn(PlaybackSpeedRange)
        // Rounding to hundredths means nearby drag frames resolve to the same value; only push real changes
        if (speed != sliderValue) {
          sliderValue = speed
          onRateChange(speed)
        }
      }

      Slider(
        value = sliderValue,
        onValueChange = setRate,
        valueRange = PlaybackSpeedRange,
        modifier = Modifier.weight(1f),
      )

      var showSpeedDialog by remember { mutableStateOf(false) }
      val editLabel = stringResource(Res.string.setting_playback_speed_option_edit)

      Text(
        text = "${sliderValue.readableHundredths}x",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
          .padding(start = 12.dp)
          .clip(RoundedCornerShape(12.dp))
          .clickable(onClickLabel = editLabel) { showSpeedDialog = true }
          .padding(horizontal = 8.dp, vertical = 12.dp)
          .width(44.dp),
      )

      if (showSpeedDialog) {
        PlaybackSpeedDialog(
          initialSpeed = sliderValue,
          onDismiss = { showSpeedDialog = false },
          onConfirm = { speed ->
            showSpeedDialog = false
            setRate(speed)
          },
        )
      }
    }
  }
}

private fun rowShape(index: Int, total: Int): Shape {
  val large = 16.dp
  val small = 4.dp
  return when {
    total == 1 -> RoundedCornerShape(large)
    index == 0 -> RoundedCornerShape(topStart = large, topEnd = large, bottomStart = small, bottomEnd = small)
    index == total - 1 ->
      RoundedCornerShape(topStart = small, topEnd = small, bottomStart = large, bottomEnd = large)
    else -> RoundedCornerShape(small)
  }
}

@Preview
@Composable
private fun PlaybackSpeedOptionsSettingPreview() {
  CampfireTheme {
    var rates by remember { mutableStateOf(listOf(1f, 1.1f, 1.25f, 1.5f, 2f)) }

    PlaybackSpeedOptionsSetting(
      rates = rates,
      onRateChange = { index, rate ->
        rates = rates.toMutableList().apply { this[index] = rate }
      },
    )
  }
}
